# Repository for the Learning Patient Cohort Retrieval (L-PCR) System

This project was written in Java, version 8. The software in this projected was developed as part of a larger project to build a Multimodal ElectRoencephalogram patient Cohort Retrieval sYstem (MERCuRY). Consequently, running the L-PCR system experiments currently requires setting up MERCuRY.

We are working on (1) preparing a minimal project providing a bare-bones index just for the L-PCR system and experiments, and (2) preparing a Docker image with the full MERCuRY environment.

The project is released in its current state mainly to serve as reference for those looking to implement some of the features described in the L-PCR paper.

# Overview
Detailed instructions are coming soon!

In the meantime, most of the magic happens in [CohortL2rProcessor.java](inquire/inquire-med/src/main/java/edu/utdallas/hltri/inquire/l2r/CohortL2rProcessor.java).

The feature extraction code for the TRECMed experiments is available [here](trecmed/src/main/java/edu/utdallas/hlt/trecmed/scripts/JamiaVectorizer2.java), and for EEGs it is available [here](mercury/mercury-core/src/main/java/edu/utdallas/hltri/mercury/jamia/scripts/JamiaVectorizer2.java).

The random forest classifiers were trained using [RankLib](https://sourceforge.net/p/lemur/wiki/RankLib/), part of the [Lemur project](https://www.lemurproject.org/indri/).


# Compiling the Code
This project is compiled with [sbt](https://www.scala-sbt.org/) v1.0.

To compile the project, type: `sbt compile`

# Indexing the data
Until we decouple the L-PCR system from MERCuRY, it is, unfortunately, necessary to prepare the data for MERCuRY.

## Preprocessing the data for MERCuRY
MERCuRY requires the dataset be processed into a custom intermediate format used by our `scribe` project.
The intermediate format is  is accomplished using the `eeg-report-annotation` module of the `eeg`project, which performs a number of linguistic annotations to the data including detection of EEG concepts, their attributes, and negation scopes.
This is all accomplished with the `edu.utdallas.hltri.eeg.io.EegJsonCorpus` class.

## Indexing the data for MERCuRY
To index the TUH EEG data, please modify & run the class `edu.utdallas.hltri.mercury.scripts.SolrIndexer`

NOTE: We are working developing a stand-alone indexer that doesn't require any of the 

# Preprocessing cohort descriptions
Before feature extraction, the cohort descriptions need to be preprocessed. 
This is accomplished using the `edu.utdallas.hltri.mercury.jamia.scripts.JamiaPreprocessor` class of the `mercury-core` module of the `mercury` project.

## Defining the Cohort Descriptions used in the experiments
Cohort description should be specified in a CSV file with the following format:
1. a header row with the columns `NAME` and `TEXT` 
2. one cohort description for each row, where the `NAME` column is an abitrary unique identifier for the cohort and `TEXT` is the natural language description of the cohort criteria.

Usage: `sbt runMain edu.utdallas.hltri.mercury.jamia.scripts.JamiaPreprocessor <path/to/cohorts> <output/folder>`
where `<path/to/cohorts>` is the path to a CSV file containing the cohort descriptions to be used in the experiments and `<output/folder>` is the destination folder where-in the intermediate processed cohort descriptions will be written

# Preparing relevance judgments
Per-visit judgments for each cohort description used in the experiments should be provided using TREC `qrels` format, i.e.:
`<cohort-name> 0 <visit-id> <judgment>` where `cohort-name` corresponds to the name used in the CSV file above, `visit-id` corresponds to the session ID in the TUH EEG corpus, and `judgment` is a positive integer indicating relevance. In our experiments, we used `0` for non-relevant, `1` for partially relevant, and `2` for relevant.

# Extracting feature vectors
Extracting feature vectors from the EEG data relies on the `JamiaVectorizer2` class from the `edu.utdallas.hltri.mercury.jamia.scripts` package.

Usage is as follows:
`sbt runMain edu.utdallas.hltri.mercury.jamia.scripts.JamiaVectorizer2 <path/to/cohort-descriptions> <path/to/qrels> <path/to/output>`

The program will write feature vectors in [SVM-rank](https://www.cs.cornell.edu/people/tj/svm_light/svm_rank.html) format.

# Learning-to-rank
The [SVM-rank](https://www.cs.cornell.edu/people/tj/svm_light/svm_rank.html) format allows a variety of tools to be used for learning to rank including [sk-learn](http://scikit-learn.org/stable/modules/generated/sklearn.datasets.load_svmlight_file.html) and [RankLib](https://sourceforge.net/p/lemur/wiki/RankLib/).

