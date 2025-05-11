# frozen_string_literal: true

require_relative 'analyzers'

Analyzers.all.each(&:save!)
