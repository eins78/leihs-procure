require_relative '../server/spec/spec_helper'

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
