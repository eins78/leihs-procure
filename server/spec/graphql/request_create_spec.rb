require 'spec_helper'
require_relative 'graphql_helper'
require_relative 'request_helper'

describe 'request' do
  context 'create' do
    context 'mutation' do
      it 'returns error if not sufficient general permissions' do
        viewer = FactoryBot.create(:user)
        category = FactoryBot.create(:category)
        FactoryBot.create(:category_viewer,
                          user_id: viewer.id,
                          category_id: category.id)

        attrs = {
          article_name: Faker::Commerce.product_name,
          budget_period: FactoryBot.create(:budget_period).id,
          category: category.id,
          requested_quantity: 1,
          room: FactoryBot.create(:room).id,
          user: viewer.id
        }

        q = <<-GRAPHQL
          mutation {
            create_request(input_data: #{hash_to_graphql attrs}) {
              id
            }
          }
        GRAPHQL

        result = query(q, viewer.id)

        expect(result['data']['request']).not_to be
        expect(result['errors'].first['exception'])
          .to be == 'UnauthorizedException'

        expect(Request.find(transform_uuid_attrs attrs)).not_to be
      end

      context 'creates as requester' do
        let :q do <<-GRAPHQL
          mutation createRequest($input: CreateRequestInput) {
            create_request(input_data: $input) {
              id
              attachments {
                value {
                  id
                }
              }
            }
          }
        GRAPHQL
        end

        let :requester do
          requester = FactoryBot.create(:user)
          FactoryBot.create(:requester_organization, user_id: requester.id)
          requester
        end

        let(:uploads) { 2.times.map { FactoryBot.create(:upload) } }

        example 'for category/no template' do
          variables = {
            input: {
              article_name: 'new request',
              budget_period: FactoryBot.create(:budget_period).id,
              category: FactoryBot.create(:category).id,
              requested_quantity: 1,
              room: FactoryBot.create(:room).id,
              motivation: Faker::Lorem.sentence,
              attachments: [{id: uploads[0].id, to_delete: false, __typename: 'Upload'},
                            {id: uploads[1].id, to_delete: true, __typename: 'Upload'}]
            }
          }

          result = query(q, requester.id, variables).deep_symbolize_keys

          request = Request.order(:created_at).reverse.first
          expect(result[:errors]).to be_nil
          data = result[:data][:create_request]
          expect(data[:id]).to be == request.id
          expect(data[:attachments][:value].count).to be == 1
          expect(Upload.count).to be == 0
          expect(Attachment.count).to be == 1
        end

        example 'from template' do
          template = FactoryBot.create(:template, {
            article_name: 'some template',
            category_id: FactoryBot.create(:category).id,
          })

          variables = {
            input: {
              budget_period: FactoryBot.create(:budget_period).id,
              template: template.id,
              requested_quantity: 1,
              room: FactoryBot.create(:room).id,
              motivation: Faker::Lorem.sentence,
              attachments: [{id: uploads[0].id, to_delete: false, __typename: 'Upload'},
                            {id: uploads[1].id, to_delete: true, __typename: 'Upload'}]
            }
          }

          result = query(q, requester.id, variables).deep_symbolize_keys

          request = Request.order(:created_at).reverse.first
          expect(result[:errors]).to be_nil
          data = result[:data][:create_request]
          expect(data[:id]).to be == request.id
          expect(data[:attachments][:value].count).to be == 1
          expect(data[:article_name][:value].count).to eq template[:article_name]
          expect(data[:motivation][:value].count).to eq variables[:input][:motivation]
          expect(Upload.count).to be == 0
          expect(Attachment.count).to be == 1
        end
      end

      context 'create for another user' do
        before :example do
          @requester = FactoryBot.create(:user)
          FactoryBot.create(:requester_organization, user_id: @requester.id)
          @category = FactoryBot.create(:category)
        end

        it 'as admin' do
          @auth_user = User.find(id: FactoryBot.create(:admin).user_id)
        end

        it 'as inspector' do
          @auth_user = FactoryBot.create(:user)
          FactoryBot.create(:category_inspector,
                            user_id: @auth_user.id,
                            category_id: @category.id)
        end

        after :example do
          attrs = {
            article_name: 'new request',
            budget_period: FactoryBot.create(:budget_period).id,
            category: @category.id,
            requested_quantity: 1,
            room: FactoryBot.create(:room).id,
            motivation: Faker::Lorem.sentence,
            user: @requester.id
          }

          q = <<-GRAPHQL
            mutation {
              create_request(input_data: #{hash_to_graphql attrs}) {
                id
              }
            }
          GRAPHQL

          result = query(q, @auth_user.id)

          request = Request.order(:created_at).reverse.first
          expect(result['data']['create_request']['id']).to be == request.id
          expect(request.user_id).to eq(@requester.id)
        end
      end

      it 'move to another category' do
        admin = FactoryBot.create(:user)
        FactoryBot.create(:admin, user_id: admin.id)

        inspector = FactoryBot.create(:user)
        category = FactoryBot.create(:category)
        new_category = FactoryBot.create(:category)
        FactoryBot.create(:category_inspector,
                          user_id: inspector.id,
                          category_id: category.id)

        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)

        ['admin', 'inspector', 'requester'].each do |user_name|
          user = binding.local_variable_get(user_name)
          request = FactoryBot.create(:request,
                                      user_id: requester.id,
                                      category_id: category.id,
                                      approved_quantity: 1,
                                      inspection_comment: Faker::Lorem.sentence,
                                      inspector_priority: 'low',
                                      order_quantity: 1)

          q = <<-GRAPHQL
            mutation changeRequestCategory($input: RequestCategoryInput!) {
              change_request_category(input_data: $input) {
                id
                category {
                  value {
                    id
                  }
                }
              }
            }
          GRAPHQL

          variables = { input: { id: request.id, category: new_category.id } }

          result = query(q, user.id, variables)

          expect(result).to be == {
            'data' => {
              'change_request_category' => {
                'id' => request.id,
                'category' => {
                  'value' => {
                    'id' => new_category.id
                  }
                }
              }
            }
          }

          request.reload
          expect(request.approved_quantity).to be_nil
          expect(request.inspection_comment).to be_nil
          expect(request.inspector_priority).to eq('medium')
          expect(request.order_quantity).to be_nil
        end
      end

      it 'move to another budget period' do
        admin = FactoryBot.create(:user)
        FactoryBot.create(:admin, user_id: admin.id)

        inspector = FactoryBot.create(:user)
        category = FactoryBot.create(:category)
        FactoryBot.create(:category_inspector,
                          user_id: inspector.id,
                          category_id: category.id)

        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)

        new_budget_period = FactoryBot.create(:budget_period)

        ['admin', 'inspector', 'requester'].each do |user_name|
          user = binding.local_variable_get(user_name)
          request = FactoryBot.create(:request,
                                      user_id: requester.id,
                                      category_id: category.id)

          q = <<-GRAPHQL
            mutation changeRequestBudgetPeriod($input: RequestBudgetPeriodInput) {
              change_request_budget_period(input_data: $input) {
                id
                budget_period {
                  value {
                    id
                  }
                }
              }
            }
          GRAPHQL

          variables = { input: { id: request.id, budget_period: new_budget_period.id } }

          result = query(q, user.id, variables)

          expect(result).to be == {
            'data' => {
              'change_request_budget_period' => {
                'id' => request.id,
                'budget_period' => {
                  'value' => {
                    'id' => new_budget_period.id
                  }
                }
              }
            }
          }
        end
      end
    end
  end
end
