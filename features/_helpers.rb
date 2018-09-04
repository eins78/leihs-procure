require_relative '../server/spec/spec_helper'

def wait_for_all_loaded
  wait_until { page.has_no_selector? '[data-icon-name="Spinner"]'}
end

def wait_until(wait_time = 60, &block)
  Timeout.timeout(wait_time) do
    until value = yield
      sleep(0.2)
    end
    value
  end
rescue Timeout::Error => e
  raise Timeout::Error.new(block.source)
end

def click_on_first(locator, options = {})
  wait_until(3) { first(:link_or_button, locator, options) }
  first(:link_or_button, locator, options).click
end

def fake_login(user)
  user = User.find(id: user) if user.is_a?(String)
  fail 'Unknown User!' unless user
  visit '/' # ensure window is open on app host, otherwise won't work!
  page.driver.browser.manage.add_cookie(name: 'leihs-fake-cookie-auth', value: user.id)
end

def take_screenshot(screenshot_dir = nil, name = nil)
  screenshot_dir ||= Rails.root.join('tmp', 'capybara')
  name ||= "screenshot_#{Time.zone.now.iso8601.tr(':', '-')}.png"

  path = File.join(Dir.pwd, screenshot_dir, name)
  FileUtils.mkdir_p(File.dirname(path))

  case Capybara.current_driver
  when :selenium
    page.driver.browser.save_screenshot(path)
  when :poltergeist
    page.driver.render(path, full: true)
  else
    fail "Taking screenshots is not implemented for \
            #{Capybara.current_driver}."
  end
end
