# frozen_string_literal: true

module Analyzers
  class OverflowTest < Base
    def mangle(messages)
      # Procedure to quickly combine array methods i.e., uniq + join
      uniq_join = proc { |array| array.uniq.join(',') }

      # The set containing all the alerts is reduced to a single hash that expresses all the data.
      with_mangled_set = messages.transform_values do |set|
        { sizes: uniq_join.call(set.map { _1['info']['size'] }),
          expressions: uniq_join.call(set.map { _1['info']['expression'] }),
          descriptions: uniq_join.call(set.map { _1['warning']['description'] }),
          abstracts: uniq_join.call(set.map { _1['warning']['abstractRepresentation'] }) }
      end

      # ...
      with_mangled_set.map do |(location, hash)|
        human_readable_description = "#{hash[:descriptions]} for sizes #{hash[:sizes]} " +
                                     "at #{location} for expressions #{hash[:expressions]} " +
                                     "holding #{hash[:abstracts]}"

        { warning: human_readable_description }
      end.then { { "#{name}" => _1 } }
    end
  end
end