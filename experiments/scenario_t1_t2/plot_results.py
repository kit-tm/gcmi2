import re
import numpy as NP
from matplotlib import pyplot as PLT
import sys, getopt
import matplotlib.lines as mlines
import matplotlib.patches as mpatches
import os
import statistics

SERVICE_NUMBERS = [0, 1, 2, 4, 8, 16]
path = ""

class FileParser:

    def __init__(self):
        self.cbench_filenames = []

    def getAllResults(self):
        regex = "^RESULT: \d switches \d+ tests min\/max\/avg\/stdev = ([0-9\.]*)\/([0-9\.]*)\/([0-9\.]*)\/([0-9\.]*) responses\/s$"
        result = {}

        for i in SERVICE_NUMBERS:
            with open('cbench_throughput_{}.log'.format(i)) as currentFile:
                lines = currentFile.read()
                match = re.search(regex, lines, re.MULTILINE)
                res = [float(match.group(1)), float(match.group(2)), float(match.group(3)), float(match.group(4))]
                result[str(i)] = [int(res[2]), int(res[3])]

        return result


class Plotter:

    def __init__(self, data):
        self.data = data

        #font = {'family': 'normal', 'size': 18}

        #PLT.rc('font', **font)
        #PLT.rcParams['hatch.linewidth'] = 1.5

    def getDataWithIndex(self, i, x):
        return NP.array(list(map(lambda resultSet: resultSet[i][x], self.data)))

    def plot(self, outputfileName):
        fig = PLT.figure(figsize=(3.5, 3))
        PLT.xlabel('Number of GCMI Apps')
        PLT.ylabel('Responses per s')

        # y axis between 0 and 4000
        #PLT.yticks(NP.arange(0, 60000, 10000.0))
        PLT.gca().set_ylim([0, 65000])

        blue = "#1f77b4"
        orange = "#ff7f0e"

        y_values = []
        x_values = NP.arange(len(SERVICE_NUMBERS))
        x = 0

        for i in SERVICE_NUMBERS:
            avg = self.data[str(i)][0]
            stdev = self.data[str(i)][1]

            PLT.errorbar(x, avg, stdev, fmt='', color=orange, elinewidth=4)
            PLT.plot(x, avg, color=blue, marker='o', markersize=4, linestyle='-')
            y_values.append(avg)

            x += 1


        PLT.gca().set_xticks(x_values)
        PLT.gca().set_xticklabels(['direct', 1, 2, 4, 8, 16])

        PLT.gca().yaxis.grid(True)
        PLT.plot(x_values, y_values, linewidth=2)

        # Draw legend
        PLT.rcParams['legend.fontsize'] = '10'
        blue_line = mlines.Line2D([], [], color=blue, marker='o', markersize=4, label='mean')
        orange_error = mpatches.Patch(color=orange, label='standard deviation', linewidth=1)
        PLT.legend(handles=[blue_line, orange_error], loc='upper right')

        PLT.tight_layout()

        outputfileName = "" + outputfileName

        fig.savefig(path + "/" + outputfileName, bbox_inches='tight')
        print("saved plot to " + outputfileName)


def main(argv):
    try:
        opts, args = getopt.getopt(argv, "hp:", ["path="])
    except getopt.GetoptError:
        print('plot_throughput_results.py -p <path>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print('plot_throughput_results.py -p <path>')
            sys.exit()
        elif opt in ("-p", "--path"):
            global path
            path = arg

    outputfile = "throughput_plot.pdf"

    fileParser = FileParser()
    data = fileParser.getAllResults()

    plotter = Plotter(data)
    plotter.plot(outputfile)


if __name__ == "__main__":
    main(sys.argv[1:])
