# frozen_string_literal: true

require 'json'
require 'set'

module Analyzers
  class Base
    REPORT_NAME = 'report.json'
    STRING_TO_REMOVE = '[GENERIC]'

    # It should be configurable, but for simplicity's sake it's been hardcoded here.
    BASE_DIR = Pathname.new '/lisa/warnings/'

    def initialize(output)
      @output = output
    end

    # Name of the current analysis.
    def name
      @name ||= @output.basename.to_s
    end

    # Calculates the JSON representation of the warning reported by LiSA analysis.
    def children
      @children ||= @output
                      .children
                      .map do |analysis|

        # Extract the 'warnings' array from a JSON report file within the analysis. Essentially:
        #   1.  Get all children of the analysis.
        #   2.  Filter to include only file entries.
        #   3.  Filter for the file whose basename matches the REPORT_NAME constant.
        #   4.  Take the first matching file (assuming there's only one report file).
        #   5.  Read the content of the file and parse it as JSON.
        #   6.  Fetch the value associated with the 'warnings' key in the parsed JSON.
        warnings = analysis
                     .children
                     .filter(&:file?)
                     .filter { _1.basename.to_s == REPORT_NAME }
                     .first
                     .then { JSON.parse(_1.read) }
                     .fetch('warnings')

        # The LiSA static parser will append the [GENERIC] identifier to custom strings, invalidating valid JSON.
        messages = warnings
                     .map { _1['message'] }
                     .map { _1.sub STRING_TO_REMOVE, '' }
                     .map { JSON.parse _1 }
                     .reduce({}) do |result, hash|

          # This will group the messages under the same code position. The steps are as follows:
          #   1.  Extract the 'location' value from the 'info' hash within the current 'hash'.
          #   2.  Initialize a new Set for the 'location' in the 'result' hash if it doesn't already exist.
          #   3.  Create a new Set by merging the existing Set for the 'location' with a new hash where the 'location'
          #       key has been removed from the 'info' hash.
          #   4.  Update the 'result' hash with the new Set for the current 'location'.
          location = hash['info']['location']
          result[location] = Set.new unless result.key? location
          new_set = result[location] + [hash.merge('info' => hash['info'].except('location'))]
          result.tap { result[location] = new_set }
        end

        { filename: analysis.basename.to_s,
          messages: messages }
      end
    end

    def save!
      # Define the entrypoint directory by joining the base directory with the given 'name'.
      entrypoint = BASE_DIR
                     .join(name)
                     .tap { _1.mkdir unless _1.exist? }

      # Iterate over the 'children', transforming each entry and writing the result to a file.
      children
        .map { [mangle(_1[:messages]), _1[:filename]] }
        .each do |result, filename|
        # Construct the full path for the output file by joining the 'entrypoint' with the 'filename' and write the
        # 'result' (which is expected to be a data structure) to the file as JSON.
        entrypoint
          .join("#{filename}.json")
          .write(result.to_json)
      end
    end

    def mangle(messages)
      raise NoMethodError
    end
  end
end
