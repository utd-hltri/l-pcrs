#!/bin/zsh

b=/home/travis/work/trecmed_l2r/experiments/jamia_2016_v4_2012/kcv

for i in $(seq 01 10); do 
	java -Xmx12g -jar ~/code/third-party/RankLib/target/RankLib-2.8-SNAPSHOT.jar \
		-ranker 8 \
		-metric2t MAP \
		-load $b/f$i.forest \
		-rank $b/f$i.test_vectors.svml \
		-norm zscore \
		-score $b/f$i.scores
	sbt-run edu.utdallas.hlt.trecmed.scripts.ConvertRankLibScores \
		$b/f$i.scores \
		$b/f$i.test_vectors.svml \
		$b/f$i.trec_submission.tsv
	trec_eval \
		-m all_trec \
		-q ~/work/trecmed/official/2012_treceval_qrels.txt \
		$b/f$i.trec_submission.tsv | tee $b/f$i.trec_eval.txt
done
