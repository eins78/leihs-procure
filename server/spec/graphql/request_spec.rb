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
          organization: FactoryBot.create(:organization).id,
          requested_quantity: 1,
          room: FactoryBot.create(:room).id,
          user: viewer.id
        }

        q = <<-GRAPHQL
          mutation {
            new_request(input_data: #{hash_to_graphql attrs}) {
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

      it 'creates if required general permission exist' do
        admin = FactoryBot.create(:user)
        FactoryBot.create(:admin, user_id: admin.id)

        inspector = FactoryBot.create(:user)
        category = FactoryBot.create(:category)
        FactoryBot.create(:category_inspector,
                          user_id: inspector.id,
                          category_id: category.id)

        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)

        attrs = {
          budget_period: FactoryBot.create(:budget_period).id,
          category: category.id,
          organization: FactoryBot.create(:organization).id,
          requested_quantity: 1,
          room: FactoryBot.create(:room).id
        }

        ['admin', 'inspector', 'requester'].each do |user_name|
          user = binding.local_variable_get(user_name)

          attrs2 = attrs.merge(article_name: user_name,
                               user: user.id)

          q = <<-GRAPHQL
            mutation {
              new_request(input_data: #{hash_to_graphql attrs2}) {
                id
              }
            }
          GRAPHQL

          result = query(q, user.id)

          request = Request.find(transform_uuid_attrs attrs2)
          expect(result).to be == {
            'data' => {
              'new_request' => {
                'id' => request.id
              }
            }
          }
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
                                      category_id: category.id)

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
            mutation {
              change_request_budget_period(input_data: {
                id: "#{request.id}",
                budget_period: "#{new_budget_period.id}"
              }) {
                id
                budget_period {
                  value {
                    id
                  }
                }
              }
            }
          GRAPHQL

          result = query(q, user.id)

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

  context 'update' do
    context 'mutation' do
      it 'returns error if not sufficient general permissions' do
        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)
        viewer = FactoryBot.create(:user)
        FactoryBot.create(:category_viewer, user_id: viewer.id)

        request = FactoryBot.create(:request)

        q = <<-GRAPHQL
          mutation {
            request(input_data: {
              id: "#{request.id}",
              article_name: "test"
            }) {
              id
            }
          }
        GRAPHQL

        [requester, viewer].each do |user|
          result = query(q, user.id)

          expect(result['data']['request']).not_to be
          expect(result['errors'].length).to be 1
          expect(result['errors'].first['exception'])
            .to be == 'UnauthorizedException'

          expect(request).to be == request.reload
        end
      end
    end

    it 'updates if required general permission exists' do
      admin = FactoryBot.create(:user)
      FactoryBot.create(:admin, user_id: admin.id)

      inspector = FactoryBot.create(:user)
      category = FactoryBot.create(:category)
      FactoryBot.create(:category_inspector,
                        user_id: inspector.id,
                        category_id: category.id)

      requester = FactoryBot.create(:user)
      FactoryBot.create(:requester_organization, user_id: requester.id)

      request = FactoryBot.create(:request,
                                  user_id: requester.id,
                                  category_id: category.id)

      ['admin', 'inspector', 'requester'].each do |user_name|
        user = binding.local_variable_get(user_name)

        q = <<-GRAPHQL
          mutation {
            request(input_data: {
              id: "#{request.id}",
              article_name: "#{user_name}"
            }) {
              id
            }
          }
        GRAPHQL

        result = query(q, user.id)
        expect(result).to eq({
          'data' => {
            'request' => {
              'id' => request.id
            }
          }
        })
        expect(request.reload.article_name).to be == user_name
      end
    end

    context 'delete' do
      context 'mutation' do
        it 'returns error if not sufficient general permissions' do
          requester = FactoryBot.create(:user)
          FactoryBot.create(:requester_organization, user_id: requester.id)
          viewer = FactoryBot.create(:user)
          category = FactoryBot.create(:category)
          FactoryBot.create(:category_viewer,
                            user_id: viewer.id,
                            category_id: category.id)

          request = FactoryBot.create(:request)

          q = <<-GRAPHQL
            mutation deleteRequest($input: DeleteRequestInput) {
              delete_request(input_data: $input)
            }
          GRAPHQL

          variables = { input: { id: request.id } }

          [requester, viewer].each do |user|
            result = query(q, user.id, variables)

            expect(result['data']['delete_request']).to be_nil
            expect(result['errors'].first['exception'])
              .to be == 'UnauthorizedException'

            expect(request).to be == request.reload
          end
        end
      end

      it 'updates if required general permission exists' do
        admin = FactoryBot.create(:user)
        FactoryBot.create(:admin, user_id: admin.id)

        inspector = FactoryBot.create(:user)
        category = FactoryBot.create(:category)
        FactoryBot.create(:category_inspector,
                          user_id: inspector.id,
                          category_id: category.id)

        requester = FactoryBot.create(:user)
        FactoryBot.create(:requester_organization, user_id: requester.id)

        ['admin', 'inspector', 'requester'].each do |user_name|
          user = binding.local_variable_get(user_name)
          request = FactoryBot.create(:request,
                                      user_id: requester.id,
                                      category_id: category.id)

        q = <<-GRAPHQL
          mutation deleteRequest($input: DeleteRequestInput) {
            delete_request(input_data: $input)
          }
        GRAPHQL

        variables = { input: { id: request.id } }

          result = query(q, user.id, variables)
          expect(result).to be == {
            'data' => {
              'delete_request' => true
            }
          }

          expect(Request.find(id: request.id)).not_to be
        end
      end
    end
  end
end
