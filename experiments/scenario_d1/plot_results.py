import statistics
import numpy as NP
from matplotlib import pyplot as PLT
import matplotlib.patches as mpatches
import sys, getopt

SERVICE_NUMBERS = [1, 2, 4, 8, 16]


class FileParser:

    def __init__(self, fileDirectory):
        self.cbench_filenames = []
        self.proxy_filenames = []
        self.controller_filenames = []
        self.currentServiceNumberId = 0
        for i in SERVICE_NUMBERS:
            self.cbench_filenames.append(fileDirectory + '/cbench_times_' + str(i) + '.txt')
            self.proxy_filenames.append(fileDirectory + '/proxy_times_' + str(i) + '.txt')
            self.controller_filenames.append(fileDirectory + '/controller_times_' + str(i) + '.txt')

    def getAllXids(self):
        xids = list(self.filecache[self.cbench_filenames[self.currentServiceNumberId]].keys())
        del xids[:5000]
        #del xids[-100:]
        return xids

    def fileToObject(self, filename):
        currentObject = {}
        for line in open(filename, "r"):
            splittedLine = line.split(' ')

            if not splittedLine[0] in currentObject:
                currentObject[splittedLine[0]] = []

            currentObject[splittedLine[0]].append(splittedLine)

        return currentObject

    def setCurrentNumberOfServices(self, number):
        self.currentServiceNumberId = number
        self.filecache = {
            self.proxy_filenames[number]: self.fileToObject(self.proxy_filenames[number]),
            self.controller_filenames[number]: self.fileToObject(self.controller_filenames[number]),
            self.cbench_filenames[number]: self.fileToObject(self.cbench_filenames[number])
        }

    def getLinesWithXid(self, xid, filename):
        return self.filecache[filename][xid]

    def getCbenchTime(self, xid, startOrEnd):
        valuePos = 1 if startOrEnd else 2
        value = self.getLinesWithXid(xid, self.cbench_filenames[self.currentServiceNumberId])[0][valuePos]
        return int(value)

    def getMeasurements(self, xid, direction, filename):
        try:
            lines = self.getLinesWithXid(xid, filename)
            lines = list(filter(lambda line: line[1] == direction, lines))
            measuredTimes = list(map(lambda line: int(line[2]), lines))
            return measuredTimes
        except KeyError as e:
            return []

    def getProxyMeasurements(self, xid, direction):
        measurements = self.getMeasurements(xid, direction, self.proxy_filenames[self.currentServiceNumberId])
        return measurements

    def getControllerMeasurement(self, xid, direction):
        try:
            measurement = self.getMeasurements(xid, direction, self.controller_filenames[self.currentServiceNumberId])
            assert len(measurement) > 0, xid + " " + direction + " not found in " + self.controller_filenames[self.currentServiceNumberId]
            return int(measurement[0])
        except AssertionError as e:
            return -1

    # CBENCH *> --- PROXY --- CONTROLLER
    def getStartTime(self, xid):
        return self.getCbenchTime(xid, True)

    # CBENCH --- *> PROXY --- CONTROLLER
    def getProxyDownStreamInTime(self, xid):
        try:
            measurements = self.getProxyMeasurements(xid, 'fromDownstream')
            assert len(measurements) == 2, "found " + str(len(measurements)) + " ProxyDownstream measurements for " + str(xid)
            return min(measurements)
        except AssertionError as e:
            return -1

    # CBENCH --- PROXY *> --- CONTROLLER
    def getProxyDownStreamOutTime(self, xid):
        try:
            measurements = self.getProxyMeasurements(xid, 'fromDownstream')
            assert len(measurements) == 2, "found " + str(len(measurements)) + " ProxyDownstream measurements for " + str(xid)
            return max(measurements)
        except AssertionError as e:
            return -1

    # CBENCH --- PROXY --- *> CONTROLLER
    def getControllerDownstreamInTime(self, xid):
        return self.getControllerMeasurement(xid, 'in')

    # CBENCH --- PROXY --- <* CONTROLLER
    def getControllerDownstreamOutTime(self, xid):
        return self.getControllerMeasurement(xid, 'out')

    # CBENCH --- PROXY <* --- CONTROLLER
    def getProxyUpStreamInTime(self, xid):
        try:
            measurements = self.getProxyMeasurements(xid, 'fromUpstream')
            assert len(measurements) == 2, "found " + str(len(measurements)) + " ProxyUpstream measurements for " + str(xid)
            return min(measurements)
        except AssertionError as e:
            return -1

    # CBENCH --- <* PROXY --- CONTROLLER
    def getProxyUpStreamOutTime(self, xid):
        try:
            measurements = self.getProxyMeasurements(xid, 'fromUpstream')
            assert len(measurements) == 2, "found " + str(len(measurements)) + " ProxyUpstream measurements for " + str(xid)
            return max(measurements)
        except AssertionError as e:
            return -1

    # CBENCH <* --- PROXY --- CONTROLLER
    def getEndTime(self, xid):
        return self.getCbenchTime(xid, False)

    def printAllMeasurementsFor(self, xid):
        print(str(self.getStartTime(xid)))
        print(str(self.getProxyDownStreamInTime(xid)))
        print(str(self.getProxyDownStreamOutTime(xid)))
        print(str(self.getControllerDownstreamInTime(xid)))
        print(str(self.getControllerDownstreamOutTime(xid)))
        print(str(self.getProxyUpStreamInTime(xid)))
        print(str(self.getProxyUpStreamOutTime(xid)))
        print(str(self.getEndTime(xid)))

    def getResultSet(self, numberOfServices):
        print("")
        print("number of Apps: " + str(SERVICE_NUMBERS[numberOfServices]))
        self.setCurrentNumberOfServices(numberOfServices)

        # everything in us
        cbench_proxy_transfertimes = []
        proxy_processtimes = []
        proxy_controller_transfertimes = []
        controller_processtimes = []
        controller_proxy_transfertimes = []
        proxy_processtimes_back = []
        proxy_cbench_transfertimes = []

        results = []

        invalid_cbench_proxy_transfertimes = 0
        invalid_proxy_processingtimes = 0
        invalid_proxy_controller_transfertimes = 0
        invalid_controller_processtimes = 0
        invalid_controller_proxy_transfertimes = 0
        invalid_proxy_downstream_processtimes = 0
        invalid_proxy_cbench_transfertimes = 0
        invalid_controller_measurements = 0
        invalid_proxy_measurements = 0
        valid_messages_measured = 0

        proxy_controller_tranfertimes_diff = []
        controller_proxy_tranfertimes_diff = []
        proxy_cbench_tranfertimes_diff = []

        for xid in self.getAllXids():
            try:
                startTime = self.getStartTime(xid)
                proxyDownstreamInTime = self.getProxyDownStreamInTime(xid)
                proxyDownstreamOutTime = self.getProxyDownStreamOutTime(xid)
                controllerDownstreamInTime = self.getControllerDownstreamInTime(xid)
                controllerDownstreamOutTime = self.getControllerDownstreamOutTime(xid)
                proxyUpstreamInTime = self.getProxyUpStreamInTime(xid)
                proxyUpstreamOutTime = self.getProxyUpStreamOutTime(xid)
                endTime = self.getEndTime(xid)

                if controllerDownstreamOutTime < 0 or controllerDownstreamInTime < 0:
                    invalid_controller_measurements += 1
                    raise AssertionError()

                if proxyDownstreamInTime < 0 or proxyDownstreamOutTime < 0 or proxyUpstreamInTime < 0 or proxyUpstreamOutTime < 0:
                    invalid_proxy_measurements += 1
                    raise AssertionError()

                if not proxyDownstreamInTime >= startTime:
                    invalid_cbench_proxy_transfertimes += 1
                    raise AssertionError()

                if not proxyDownstreamOutTime >= proxyDownstreamInTime:
                    invalid_proxy_processingtimes += 1
                    raise AssertionError()

                if not controllerDownstreamInTime >= proxyDownstreamOutTime:
                    invalid_proxy_controller_transfertimes += 1
                    proxy_controller_tranfertimes_diff.append(controllerDownstreamInTime - proxyDownstreamOutTime)
                    raise AssertionError()

                if not controllerDownstreamOutTime >= controllerDownstreamInTime:
                    invalid_controller_processtimes += 1
                    raise AssertionError()

                if not proxyUpstreamInTime >= controllerDownstreamOutTime:
                    invalid_controller_proxy_transfertimes += 1
                    controller_proxy_tranfertimes_diff.append(proxyUpstreamInTime - controllerDownstreamOutTime)
                    raise AssertionError()

                if not proxyUpstreamOutTime >= proxyUpstreamInTime:
                    invalid_proxy_downstream_processtimes += 1
                    raise AssertionError()

                if not endTime >= proxyUpstreamOutTime:
                    invalid_proxy_cbench_transfertimes += 1
                    proxy_cbench_tranfertimes_diff.append(proxyUpstreamOutTime - endTime)
                    raise AssertionError()

                cbench_proxy_transfertimes.append(proxyDownstreamInTime - startTime)
                proxy_processtimes.append(proxyDownstreamOutTime - proxyDownstreamInTime)
                proxy_controller_transfertimes.append(controllerDownstreamInTime - proxyDownstreamOutTime)
                controller_processtimes.append(controllerDownstreamOutTime - controllerDownstreamInTime)
                controller_proxy_transfertimes.append(proxyUpstreamInTime - controllerDownstreamOutTime)
                proxy_processtimes_back.append(proxyUpstreamOutTime - proxyUpstreamInTime)
                proxy_cbench_transfertimes.append(abs(endTime - proxyUpstreamOutTime))

                valid_messages_measured += 1

            except AssertionError as e:
                pass


        print("valid messages measured: {}".format(valid_messages_measured))
        print("invalid_controller_measurements: {}".format(invalid_controller_measurements))
        print("invalid_proxy_measurements: {}".format(invalid_proxy_measurements))
        print("invalid_cbench_proxy_transfertimes: {}".format(invalid_cbench_proxy_transfertimes))
        print("invalid_proxy_processingtimes: {}".format(invalid_proxy_processingtimes))
        if invalid_proxy_controller_transfertimes > 0:
            print("invalid_proxy_controller_transfertimes: {}, on average {}".format(invalid_proxy_controller_transfertimes, statistics.mean(proxy_controller_tranfertimes_diff)))
        print("invalid_controller_processtimes: {}".format(invalid_controller_processtimes))
        if invalid_controller_proxy_transfertimes > 0:
            print("invalid_controller_proxy_transfertimes: {}, on average {}".format(invalid_controller_proxy_transfertimes, statistics.mean(controller_proxy_tranfertimes_diff)))
        print("invalid_proxy_downstream_processtimes: {}".format(invalid_proxy_downstream_processtimes))
        if invalid_proxy_cbench_transfertimes > 0:
            print("invalid_proxy_cbench_transfertimes: {}, on average {}".format(invalid_proxy_cbench_transfertimes, statistics.mean(proxy_cbench_tranfertimes_diff)))


        results.append([statistics.mean(cbench_proxy_transfertimes), statistics.stdev(cbench_proxy_transfertimes)])
        results.append([statistics.mean(proxy_processtimes), statistics.stdev(proxy_processtimes)])
        results.append([statistics.mean(proxy_controller_transfertimes), statistics.stdev(proxy_controller_transfertimes)])
        results.append([statistics.mean(controller_processtimes), statistics.stdev(controller_processtimes)])
        results.append([statistics.mean(controller_proxy_transfertimes), statistics.stdev(controller_proxy_transfertimes)])
        results.append([statistics.mean(proxy_processtimes_back), statistics.stdev(proxy_processtimes_back)])
        results.append([statistics.mean(proxy_cbench_transfertimes), statistics.stdev(proxy_cbench_transfertimes)])

        print("cbench -> proxy: " + str(results[0]))
        print("proxy -> proxy: " + str(results[1]))
        print("proxy -> controller: " + str(results[2]))
        print("controller - controller: " + str(results[3]))
        print("proxy <- controller: " + str(results[4]))
        print("proxy <- proxy: " + str(results[5]))
        print("cbench <- proxy: " + str(results[6]))

        return results

    def getAllResults(self):
        results = []
        for i in range(len(SERVICE_NUMBERS)):
            results.append(self.getResultSet(i))

        return results


class Plotter:

    def __init__(self, data):
        self.data = data

        #font = {'family': 'normal', 'size': 18}

        #PLT.rc('font', **font)
        PLT.rcParams['hatch.linewidth'] = 1.5

    def getDataWithIndex(self, i, x):
        return NP.array(list(map(lambda resultSet: resultSet[i][x], self.data)))

    def plot(self, outputfileName, stdev=False):
        # result format:
        # [
        #    [
        #      [mean_proxy_transfertimes, stdev_proxy_transfertimes],
        #      [mean_proxy_processtimes, stdev_proxy_processtimes],
        #      ...
        #    ],
        #    ...
        # ]

        tupleResults = []

        x = 0
        if stdev: x = 1
        for i in range(7):
            tupleResults.append(self.getDataWithIndex(i, x))

        y = NP.row_stack(tuple(tupleResults))
        print(y)
        # this call to 'cumsum' (cumulative sum), passing in your y data,
        # is necessary to avoid having to manually order the datasets
        x = NP.arange(len(SERVICE_NUMBERS))
        y_stack = NP.cumsum(y, axis=0)  # a 3x10 array
        print(y_stack)

        fig = PLT.figure(figsize=(3.5, 3.5))
        ax1 = fig.add_subplot(111)

        colors = ["#9b59b6", "#3498db", "#95a5a6", "#e74c3c", "#34495e", "#2ecc71"]

        # draw areas
        ax1.fill_between(x, 0, y_stack[0, :], edgecolor=colors[0], facecolors='#ffffff', hatch='//')
        ax1.fill_between(x, y_stack[0, :], y_stack[1, :], edgecolor=colors[1], facecolors='#ffffff', hatch='//')
        ax1.fill_between(x, y_stack[1, :], y_stack[2, :], edgecolor=colors[2], facecolors='#ffffff', hatch='//')
        ax1.fill_between(x, y_stack[2, :], y_stack[3, :], edgecolor=colors[3], facecolors='#ffffff', hatch='xx')
        ax1.fill_between(x, y_stack[3, :], y_stack[4, :], edgecolor=colors[2], facecolors='#ffffff', hatch='\\\\')
        ax1.fill_between(x, y_stack[4, :], y_stack[5, :], edgecolor=colors[1], facecolors='#ffffff', hatch='\\\\')
        ax1.fill_between(x, y_stack[5, :], y_stack[6, :], edgecolor=colors[0], facecolors='#ffffff', hatch='\\\\')

        # ticks and margins
        fig.canvas.draw()
        PLT.xticks(NP.arange(0, 6, 1.0))
        ax1.set_xticklabels(SERVICE_NUMBERS)
        ax1.margins(0.05, 0.05)
        ax1.yaxis.grid(True)

        # y axis between 0 and 4000
        #PLT.yticks(NP.arange(0, 4000, 500.0))
        #PLT.gca().set_ylim([0, 2600])

        # titles and labels
        PLT.xlabel('Number of GCMI Apps')
        PLT.ylabel(r'Time in $\mu s$')

        PLT.tight_layout()

        if stdev:
            outputfileName = outputfileName.split('.')[0] + '_stdev.pdf'

        fig.savefig(outputfileName, bbox_inches='tight')
        print("saved plot to " + outputfileName)


def main(argv):
    path = ''
    outputfile = ''
    try:
        opts, args = getopt.getopt(argv, "p:o:", [path=", "outputfile="])
    except getopt.GetoptError:
        print('plot_results.py -p <path> -o <outputfile>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print('plot_results.py -p <path> -o <outputfile>')
            sys.exit()
        elif opt in ("-p", "--path"):
            path = arg
        elif opt in ("-o", "--outputfile"):
            outputfile = arg

    fileParser = FileParser(path)
    data = fileParser.getAllResults()

    plotter = Plotter(data)
    plotter.plot(outputfile, False)
    plotter.plot(outputfile, True)


if __name__ == "__main__":
    main(sys.argv[1:])