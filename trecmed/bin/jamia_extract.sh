sbt compile && target/start edu.utdallas.hlt.trecmed.offline.JamiaFeatures \
--qrels ~/work/trecmed/official/2011_qrels.txt \
--questions ~/work/trecmed/topics/2011_topics101-135_v4.xml.gz \
--output-dir ~/work/trecmed_l2r/experiments \
--runtag JAMIA_2011_04 \
--expand \
--force
