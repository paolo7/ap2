## Rule Applicability on RDF Triplestore Schemas

This repository contains three Java Eclipse projects:

#### 1. SchemaExpansionLogic

This project contains the core classes to compute schema expansion and rule applicability using the SCORE and CRITICAL approaches. The two approaches are defined in the `SchemaExpansionBySPARQLquery` Java class.

#### 2. SchemaQueryGenerator

This project contains the classes needed to generate random schema and rulesets. These will be stored (and reused in later runs) under the `chasebench\GPPG\` subfolder.

#### 3. SchemaExpansionExperiments

This project contains the runnable main class (in the `runBenchmark` Java class) that will start two experiments:

1. The experiment to compare the scalability of the SCORE and CRITICAL approaches.
2. The experiment to evaluate the scalability of the SCORE approach for large sets of rules and different rule sizes.

These experiments will output python code to plot the results (requires the `matplotlib` library).
