#sbt "run-main edu.utdallas.hltri.scribe.mate.MateWrapper eng -tagger /users/rmm120030/tools/srl-4.31/model/postagger.model -parser /users/rmm120030/tools/srl-4.31/model/parser.model -srl /users/rmm120030/tools/srl-4.31/model/srl.model -test $1 -out $2"

#!/bin/sh

## There are three sets of options that need, may need to, and could be changed.
## (1) deals with input and output. You have to set these (in particular, you need to provide models)
## (2) deals with the jvm parameters and may need to be changed
## (3) deals with the behaviour of the system

## For further information on switches, see the source code, or run
## java -cp srl.jar se.lth.cs.srl.Parse --help

##################################################
## (1) The following needs to be set appropriately
##################################################
#INPUT="/home/anders/corpora/conll09/eng/CoNLL2009-evaluation-English-SRLonly.txt" #evaluation corpus
INPUT=$1
LANG="eng"
##TOKENIZER_MODEL="models/eng/EnglishTok.bin.gz" #This is not used here anyway. The input is assumed to be segmented/tokenized already. 
LEMMATIZER_MODEL="/users/rmm120030/tools/srl-4.31/model/lemmatizer.model"
POS_MODEL="/users/rmm120030/tools/srl-4.31/model/postagger.model"
#MORPH_MODEL="models/ger/morph-ger.model" #Morphological tagger is not applicable to English. Fix the path and uncomment if you are running german.
PARSER_MODEL="/users/rmm120030/tools/srl-4.31/model/parser.model"
SRL_MODEL="/users/rmm120030/tools/srl-4.31/model/srl.model"
OUTPUT=$2

##################################################
## (2) These ones may need to be changed
##################################################
JAVA="java" #Edit this i you want to use a specific JRE.
MEM="4g" #Memory for the JVM, might need to be increased for large corpora.
CP="mate_tools_wrapper_2.10-1.0.jar:lib/anna-3.3.jar:lib/liblinear-1.51-with-deps.jar:lib/opennlp-tools-1.4.3.jar:lib/maxent-2.5.2.jar:lib/trove.jar:lib/seg.jar:lib/srl.jar"
JVM_ARGS="-cp $CP -Xmx$MEM"

##################################################
## (3) The following changes the behaviour of the system
##################################################
#RERANKER="-reranker" #Uncomment this if you want to use a reranker too. The model is assumed to contain a reranker. While training, the corresponding parameter has to be provided.
#NOPI="-nopi" #Uncomment this if you want to skip the predicate identification step.



##################################################

CMD="$JAVA $JVM_ARGS edu.utdallas.hltri.scribe.mate.MateWrapper $LANG $NOPI $RERANKER -tagger $POS_MODEL -parser $PARSER_MODEL -srl $SRL_MODEL -test $INPUT -out $OUTPUT"

if [ "$TOKENIZER_MODEL" != "" ]; then
  CMD="$CMD -token $TOKENIZER_MODEL"
fi

if [ "$LEMMATIZER_MODEL" != "" ]; then
  CMD="$CMD -lemma $LEMMATIZER_MODEL"
fi

if [ "$MORPH_MODEL" != "" ]; then
  CMD="$CMD -morph $MORPH_MODEL"
fi

echo "Executing: $CMD"

$CMD
