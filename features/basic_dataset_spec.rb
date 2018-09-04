require_relative '_helpers'

describe 'viewing the basic dataset', type: :feature do

  let(:basic_dataset) do
    admin_user = create(:user, {firstname: 'Procure', lastname: 'Admin'})
    create(:admin, user_id: admin_user.id)

    create(:settings, {
      contact_url: 'https://example.com/help/procurement',
      inspection_comments: ['OVER 9000', 'OK', 'NOT OK'].to_json
    })

    # Users
    users = {
      requesters: {
        requester_audio_music: { firstname: 'Requester', lastname: 'Audio-Music' },
        requester_audio_sounddesign: { firstname: 'Requester', lastname: 'Audio-Sounddesign' },
        requester_video_film: { firstname: 'Requester', lastname: 'Video-Film' },
        requester_video_tv: { firstname: 'Requester', lastname: 'Video-TV' }
      },
      inspectors: {
        inspector_audio_music: { firstname: 'Viewer', lastname: 'Audio-Music-Equip' },
        inspector_video_film: { firstname: 'Viewer', lastname: 'Video-Film-Equip'},
        inspector_computers: { firstname: 'Viewer', lastname: 'Computers' },
      },
      viewers: {
        viewer_audio_music: { firstname: 'Viewer', lastname: 'Audio-Music-Equip' },
        viewer_video_film: { firstname: 'Viewer', lastname: 'Video-Film-Equip'},
        viewer_computers: { firstname: 'Viewer', lastname: 'Computers' },
      }
    }.each do |type, list|
      list.each do |login, attrs|
        create(:user, attrs.merge(login: login))
      end
    end

    # Requesters/Orgs
    [
      {
        user_login: 'requester_audio_music',
        org: { name: 'Music', parent: 'Audio'}
      },
      {
        user_login: 'requester_audio_sounddesign',
        org: { name: 'Sounddesign', parent: 'Audio'}
      },
      {
        user_login: 'requester_video_film',
        org: { name: 'Film', parent: 'Video'}
      },
      {
        user_login: 'requester_video_tv',
        org: { name: 'TV', parent: 'Video'}
      }
    ].each do |ro|
      create(:requester_organization, {
        user_id: User.find(login: ro[:user_login]).id,
        organization_id: create(:organization, ro[:org].merge(parent: create(:department, {name: ro[:org][:parent]}))).id
      })
    end

    # BudgetPeriods

    # TODO: decide if time travel or not
    # [
    #   { name: '1999', inspection_start_date: '1998-08-01', end_date: '1998-12-31' },
    #   { name: '2000', inspection_start_date: '1999-08-01', end_date: '1999-12-31' },
    #   { name: '2001', inspection_start_date: '2000-08-01', end_date: '2000-12-31' }
    # ].each do |bp|
    this_year = Date.today.beginning_of_year
    [
      { name: 'last year', year: this_year - 2.years},
      { name: 'this year', year: this_year - 1.years },
      { name: 'next year', year: this_year }
    ].each do |bp|
      create(:budget_period, {
        name: bp[:name],
        inspection_start_date: bp[:year] + 7.months,
        end_date:  bp[:year] + 12.months - 1.day
      })
    end

    # MainCategories and (Sub-)Categories
    [
      {
        name: 'Audio and Music Equipment',
        categories: [ 'Musical Instruments', 'Audio Interfaces' ]
      },
      {
        name: 'Video and Film Equipment',
        categories: [ 'Analog Cameras', 'Digital Cameras', 'Lighting Equipment' ]
      },
      {
        name: 'Computers',
        categories: [ 'Desktops', 'Laptops', 'Mainframes' ]
      }
    ].each do |attrs|
      mc = create(:main_category, { name: attrs[:name] })
      attrs[:categories].each do |name|
        create(:category, { name: name, main_category_id: mc.id})
      end
    end

    # Inspectors
    [
      { user_login: 'inspector_audio_music', cat_names: [ 'Musical Instruments', 'Audio Interfaces' ] },
      { user_login: 'inspector_video_film', cat_names: [ 'Analog Cameras', 'Digital Cameras', 'Lighting Equipment' ] },
      { user_login: 'inspector_computers', cat_names: [ 'Desktops', 'Laptops', 'Mainframes' ]},
    ].each do |attrs|
      attrs[:cat_names].each do |scn|
        sc = Category.find(name: scn)
        create(:category_inspector, {
          user_id: User.find(login: attrs[:user_login]).id,
          category_id: sc.id
          })
      end
    end

    # Viewers
    [
      { user_login: 'viewer_audio_music', cat_names: [ 'Musical Instruments', 'Audio Interfaces' ] },
      { user_login: 'viewer_video_film', cat_names: [ 'Analog Cameras', 'Digital Cameras', 'Lighting Equipment' ] },
      { user_login: 'viewer_computers', cat_names: [ 'Desktops', 'Laptops', 'Mainframes' ]},
    ].each do |attrs|
      attrs[:cat_names].each do |scn|
        sc = Category.find(name: scn)
        create(:category_viewer, {
          user_id: User.find(login: attrs[:user_login]).id,
          category_id: sc.id
          })
      end
    end

    # Templates
    [
      { name: 'Commodore 64', price: 595, category: 'Desktops' },
      { name: 'Apple Powerbook 5300', price: 2_300, category: 'Laptops' },
      { name: 'Cray-1', number: 'Cray-1A', price: 7_900_000, category: 'Mainframes' },

      { name: 'Camera 35mm', price: 15_000, category: 'Analog Cameras' },
      { name: 'Camera 70mm ', price: 75_000, category: 'Analog Cameras' },

      { name: 'Camera HDTV', price: 1_499, category: 'Digital Cameras' },
      { name: 'Camera 4K', price: 3_799, category: 'Digital Cameras' },

      { name: 'Floodlight LED', price: 250, category: 'Lighting Equipment' },
      { name: 'PAR Light', price: 400, category: 'Lighting Equipment' },
      { name: 'Beautydish', price: 100, category: 'Lighting Equipment' }
    ].each do |attrs|
      create(:template, {
        article_name: attrs[:name],
        price_cents: attrs[:price] * 100,
        price_currency: 'CHF',
        category_id: Category.find(name: attrs[:category]).id
      })
    end

    # Requests
    # TMP MOCK DATA
    # binding.pry
    RequesterOrganization.map do |ro|
      BudgetPeriod.all.each do |bp|
        Category.all.each do |sc|
          create(:request, {
            budget_period_id: bp.id,
            category_id: sc.id,
            user_id: ro.user.id,
            organization_id: ro.organization.id,
            price_cents: rand(10_000) * 100
          })
        end
      end
    end

    { admin_user: admin_user }
  end

  scenario 'look at it and gather screenshots' do
    # requesting phase of BudgetPeriod 2 of 3
    # Timecop.freeze('1999-05-01'.to_date)

    dataset = basic_dataset
    fake_login(dataset[:admin_user])
    visit '/requests'
    expect(page).to have_selector('h4', text: '0 Anträge')
    binding.pry

    visit '/admin/users'
    docs_screenshot("basic_dataset/admin_users_index")
    visit '/admin/budget-periods'
    docs_screenshot("basic_dataset/admin_budget_periods_index")
    visit '/admin/categories'
    docs_screenshot("basic_dataset/admin_categories_index")
    visit '/admin/organizations'
    docs_screenshot("basic_dataset/admin_organizations_index")
    visit '/templates'
    docs_screenshot("basic_dataset/templates_index")

    User.all.each do |user|
      fake_login(user)
      visit '/requests'
      reset_filters
      click_refresh_and_wait
      # binding.pry
      docs_screenshot("basic_dataset/dashboard_default_as_user_#{user.firstname}_#{user.lastname}")
    end
  end

end

def docs_screenshot(name)
  wait_for_all_loaded
  take_screenshot('docs/images/screenshots', "#{name}.png")
end

def reset_filters
  click_on 'Filter zurücksetzen'
  all('select[name="budgetPeriods"] option').each(&:select_option)
end

def click_refresh_and_wait
  btn = find_button 'refresh data'
  btn.click
  icon = btn.find('[data-hook-icon-name="Reload"]')
  wait_until { icon.not_matches_selector? '.fa-spin'}
end
