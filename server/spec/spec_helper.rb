require 'active_support/all'
require 'config/constants'
require 'config/database'
require 'config/factories'
require 'config/metadata_extractor'
require 'config/web'
require 'timecop'
require 'pry'

BROWSER_WINDOW_SIZE = [ 1200, 800 ]

RSpec.configure do |config|

  Capybara.current_session.current_window.resize_to(*BROWSER_WINDOW_SIZE)

end
