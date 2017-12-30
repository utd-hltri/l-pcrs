#1/bin/bash
cp ~/data/eeg/brat/100/"$1".* ~/data/eeg/brat/gold/
run-main.sh edu.utdallas.hltri.eeg.EEG ~/data/eeg/train ~/data/eeg/brat/100 $1
