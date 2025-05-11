# frozen_string_literal: true

module Analyzers
  class DivisionByZeroTest < Base
    def mangle(messages)
      messages.transform_values do |set|
        { descriptions: set.map { _1['warning']['description'] },
          expressions: set.map { _1['info']['expression'] } }
      end.map do |(location, hash)|
        { warning: "#{hash[:descriptions]} at #{location} for #{hash[:expressions]}" }
      end.then { { "#{name}" => _1 } }
    end
  end
end