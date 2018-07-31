class Upload < Sequel::Model(:procurement_uploads)
end

FactoryBot.define do
  f_name = 'secd.pdf'
  f_path = "spec/files/#{f_name}"
  me = MetadataExtractor.new(f_path)

  factory :upload, class: Upload do
    filename f_name
    content_type 'application/pdf'
    size 56000
    content Base64.encode64(File.new(f_path).read)
    metadata me.data.to_display_hash.to_json
  end
end
