package it.unive.scsr.cp;

import it.unive.lisa.analysis.dataflow.DefiniteDataflowDomain;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.scsr.CProp;

public record ConstantPropagationData(Identifier id, DefiniteDataflowDomain<CProp> domain) {}
