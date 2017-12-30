from __future__ import print_function
from __future__ import division

import os, sys

from nltk.translate import bleu_score

smoothing_function = bleu_score.SmoothingFunction().method2
weights = (.25, .25, .25, .25)

def main(argv):
    basedir = argv[0]

    guess_dir = os.path.join(basedir, 'guess')
    gold_dir = os.path.join(basedir, 'gold')

    guesses = []
    golds = []

    scores = []
    for _, _, files in os.walk(guess_dir):
        for file in files:
            with open(os.path.join(guess_dir, file), 'r') as guess_file:
                guess = guess_file.read().split()
            with open(os.path.join(gold_dir, file), 'r') as gold_file:
                gold = gold_file.read().split()

            if len(guess) > 1 and len(gold) > 1:
                try:
                    score = bleu_score.sentence_bleu([gold], guess, weights=weights, smoothing_function=smoothing_function)
                    print("BLEU(%s) = %f" % (file, score))
                    guesses.append(guess)
                    golds.append([gold])
                    scores.append(score)
                except ZeroDivisionError:
                    pass


    print("========================================\n")
    print("Macro Average BLEU = %f\n" % (sum(scores) / len(scores)))
    print("Micro Average BLEU = %f\n" % bleu_score.corpus_bleu(golds, guesses, weights=weights, smoothing_function=smoothing_function))

if __name__ == "__main__":
    main(sys.argv[1:])