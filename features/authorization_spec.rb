require_relative '_helpers'

describe 'visiting the web ui', type: :feature do

  let (:unallowed_user) { create(:user) }
  let (:admin_user) { u = create(:user); create(:admin, user_id: u.id); u }
  let (:requester_user) { User.find(id: create(:requester_organization).user_id) }
  let (:inspector_user) { User.find(id: create(:category_inspector).user_id) }
  let (:viewer_user) { User.find(id: create(:category_viewer).user_id) }

  scenario 'as admin user - it works' do
    fake_login(admin_user)
    visit '/requests'
    expect(page).to have_selector('h4', text: '0 Anträge')
  end

  scenario 'as user of roles requester, inspector viewer (each) - it shows error message' do
    [requester_user, inspector_user, viewer_user ].each do |user|
      fake_login(user)
      visit '/requests'
      expect(page).to have_selector('h4', text: '0 Anträge')
    end
  end

  scenario 'as unknown or non-privileged user (each)- it shows error message and home link' do
    [nil, unallowed_user].each do |user|
      fake_login(user) if user
      visit '/requests'
      within('.ui-app') do
        expect(page).to have_selector('h1', text: 'ERROR')
        expect(page).to have_selector('a[href="/"]') # home link
        expect(page).to have_content '"message": "Not authorized to access procurement!"'
      end
    end
  end
end
