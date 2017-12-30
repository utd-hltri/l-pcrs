#!/usr/bin/env python
'''
Created on Oct 16, 2011

@author: Travis
'''
import numpy as np
import matplotlib.pyplot as plt

ours = [
    (136, 0.0492),
    (137, 0.0000),
    (139, 0.5641),
    (140, 0.4229),
    (141, 0.2878),
    (142, 0.1229),
    (143, 0.5479),
    (144, 0.0144),
    (145, 0.3105),
    (146, 0.0000),
    (147, 0.0895),
    (148, 0.5487),
    (149, 0.0007),
    (150, 0.2072),
    (151, 0.0212),
    (152, 0.0172),
    (153, 0.0001),
    (154, 0.0159),
    (155, 0.0801),
    (156, 0.0606),
    (157, 0.1890),
    (158, 0.7077),
    (160, 0.1160),
    (161, 0.5427),
    (162, 0.0319),
    (163, 0.0159),
    (164, 0.5854),
    (165, 0.1771),
    (167, 0.0000),
    (168, 0.0065),
    (169, 0.3743),
    (170, 0.6637),
    (171, 0.4597),
    (172, 0.0123),
    (173, 0.0108),
    (174, 0.1940),
    (175, 0.3622),
    (176, 0.0548),
    (177, 0.0037),
    (178, 0.7938),
    (179, 0.0000),
    (180, 0.3314),
    (181, 0.0420),
    (182, 0.0662),
    (183, 0.1330),
    (184, 0.4003),
    (185, 0.0406)]

best = [
    0.5724,
    0.0558,
    0.6906,
    0.5870,
    0.5068,
    0.4352,
    0.6247,
    0.1723,
    0.6206,
    0.5381,
    0.2012,
    0.5584,
    0.0920,
    0.8667,
    0.6089,
    0.4293,
    0.5716,
    0.4681,
    0.2033,
    0.1598,
    0.4214,
    0.7885,
    0.2486,
    0.8444,
    0.0784,
    0.3203,
    0.7426,
    0.4974,
    0.4324,
    0.1458,
    0.5277,
    0.8474,
    0.6934,
    0.2474,
    0.4733,
    0.4167,
    0.7323,
    0.2835,
    0.4027,
    0.9055,
    0.4033,
    0.5481,
    0.4044,
    0.1062,
    0.3542,
    0.5762,
    0.6727]

median = [
    0.0493,
    0.0000,
    0.2046,
    0.2554,
    0.1307,
    0.1492,
    0.4673,
    0.0804,
    0.4413,
    0.0132,
    0.0786,
    0.3910,
    0.0291,
    0.5237,
    0.0058,
    0.0475,
    0.2226,
    0.0556,
    0.0668,
    0.0560,
    0.0477,
    0.2801,
    0.0624,
    0.0883,
    0.0463,
    0.0980,
    0.4476,
    0.2518,
    0.0000,
    0.0355,
    0.4357,
    0.5378,
    0.2146,
    0.0699,
    0.0423,
    0.0881,
    0.3662,
    0.0403,
    0.0385,
    0.6168,
    0.0010,
    0.2910,
    0.0265,
    0.0758,
    0.0935,
    0.2946,
    0.0813]

# Join all our scores into tuples by topic
values = [(ours[i][0], ours[i][1], best[i], median[i]) for i in range(len(ours))]

# Sort everything by the median topic's score
values.sort(key = lambda x: x[3], reverse = True)

# Number of elements (visits)
N = len(ours)

# Peel each line from the tuple
scores = [value[1] for value in values]
best = [value[2] for value in values]
median = [value[3] for value in values]

# Horizontal locations for each group
ind = np.arange(N)

# Width of each bar
width = .75

# Plot our bar charts on the graph
best_graph = plt.bar(ind,           # Bar x-offsets
    best,              # Bar heights
    width,             # Bar widths
    color='#91D515')   # Bar color

our_graph = plt.bar(ind,            # Bar x-offsets
    scores,            # Bar heights
    width,             # Bar widths
    color='#8AD1D8')   # Bar color

med_graph = plt.bar(ind,            # Bar x-offsets
    median,            # Bar heights
    width,             # Bar widths
    ec='#7B0A04',      # Edge color
    hatch='///',        # Edge pattern
    fill=False)        # Transparent bar

# Set the label for the y-axis
plt.ylabel('Scores')

# Set the title
plt.title('Topic Scores')

# Set the x-axis labels
plt.xlabel("Topics")
plt.xticks(
    ind + width * .5,
    ["%02d" % (value[0] % 100) for value in values])

# Create a legend
plt.legend(
    (our_graph[0], best_graph[0], med_graph[0]),
    ('Score', 'Best', 'Median'))

# Render the graph
plt.show()
