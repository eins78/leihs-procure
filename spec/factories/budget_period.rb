class BudgetPeriod < Sequel::Model(:procurement_budget_periods)
end

FactoryBot.define do
  factory :budget_period do
    inspection_start_date { DateTime.now + 30 }
    end_date { DateTime.now + 90 }
    name { end_date.year }
  end
end
