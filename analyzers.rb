# frozen_string_literal: true

require 'pathname'
Pathname.glob('analyzers/**/*.rb').each { |f| require_relative f }

module Analyzers
  # Base folder where the LiSA analysis will be saved.
  BASE_FOLDER = '/lisa/outputs'

  class << self
    def all
      # Constants declared by this module.
      constants = Analyzers.constants

      # Creates a lambda function that normalizes a given name by converting it to an underscored, lowercase form
      # (following Rails' underscore inflector) and then removing all underscores.
      normalize_name = lambda do |name|
        # Makes an underscored, lowercase form from the expression in the string.
        # Taken from https://api.rubyonrails.org/classes/ActiveSupport/Inflector.html#method-i-underscore.
        underscored_name = name
                             .gsub(/::/, '/')
                             .gsub(/([A-Z]+)([A-Z][a-z])/, '\1_\2')
                             .gsub(/([a-z\d])([A-Z])/, '\1_\2')
                             .tr("-", "_")
                             .downcase

        # Remove the '_' character from the name.
        underscored_name.gsub('_', '')
      end

      dir_names = constants
                    .map(&:to_s)
                    .map { |name| normalize_name.call(name) }

      path_names = Pathname
                     .glob("#{BASE_FOLDER}/*/")
                     .filter(&:directory?)
                     .filter { dir_names.include? _1.basename.to_s }

      # Get all defined constants, resolve them to their actual class objects within the Analyzers module, filter out
      # non-class constants, and then further filter for classes that are descendants of Analyzers::Base. Finally, map
      # each valid analyzer class to an instance of itself.
      constants
        .map { Object.const_get "#{Analyzers}::#{_1}" }
        .filter { _1.is_a? Class }
        .filter { _1 < Analyzers::Base }
        .map do |klass|

        # Normalize the current analyzer's class name to a consistent format. Then, search within 'path_names' for an
        # entry whose basename matches this normalized name.
        normalized_klass_name = normalize_name.call(klass.name).split('/').last
        analysis_type = path_names.find { _1.basename.to_s == normalized_klass_name }

        # If no matching analysis type (e.g., a corresponding file or directory) is found in 'path_names' for the
        # current analyzer class, raise an error indicating the missing type.
        if analysis_type.nil?
          raise StandardError,
                "No such analysis type: #{normalized_klass_name}"
        end

        # Create a new instance of the current analyzer class ('klass'), passing the found 'analysis_type' to its
        # constructor.
        klass.new(analysis_type)
      end
    end
  end
end