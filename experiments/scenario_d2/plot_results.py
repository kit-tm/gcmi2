import statistics
import sys
import numpy as np
import matplotlib.pyplot as plt
import re
import pandas as pd
import os
from matplotlib.patches import Patch

def fileToObject(filename):
    currentObject = {}
    for line in open(filename, "r"):
        splittedLine = line.split(' ')

        if not splittedLine[0] in currentObject:
            currentObject[splittedLine[0]] = []

        currentObject[splittedLine[0]].append(int(splittedLine[2]))

    return currentObject

def checkresults(results):
    valid_result_counter = 0
    invalid_result_counter = 0
    for xid in results:
        if len(results[xid]) == 2: valid_result_counter += 1
        else: invalid_result_counter += 1

    print(str(valid_result_counter) + " valid results")
    print(str(invalid_result_counter) + " invalid results")

def get_results_for_file(filename):
    results = fileToObject(filename)
    xids = list(results.keys())
    del xids[:1000]
    checkresults(results)

    measured_delays = []
    for xid in xids:
        if len(results[xid]) == 2:
            measured_delays.append(abs(results[xid][0] - results[xid][1]))

    mean = statistics.mean(measured_delays)
    stdev = statistics.stdev(measured_delays)
    print("{} delays in file {}".format(len(measured_delays), filename))
    print("mean: {} stdev: {}".format(mean, stdev))

    return mean, stdev

def get_avg_from_cbench_result(filename):
    filenames = []
    for test_number in [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]:
        newFileName = filename.replace('.log', '_{}.log'.format(test_number))
        if os.path.isfile(newFileName): filenames.append(newFileName)

    regex = "^RESULT: \d switches \d+ tests min\/max\/avg\/stdev = ([0-9\.]*)\/([0-9\.]*)\/([0-9\.]*)\/([0-9\.]*) responses\/s$"

    if len(filenames) == 0:
        filenames = [filename]
        print("only one result file found for " + filename)

    results = []
    for currentFileName in filenames:
        with open(currentFileName) as currentFile:
            lines = currentFile.read()
            match = re.search(regex, lines, re.MULTILINE)
            res = [float(match.group(1)), float(match.group(2)), float(match.group(3)), float(match.group(4))]
            if int(res[2]) == 0: print("result from " + currentFileName + " is 0!")
            results.append(int(res[2]))

    result = statistics.mean(results)
    print(str(result))
    return result

def get_pybench_result(filename):
    delays = []
    for line in open(filename, "r"):
        splitted = line.split(' ')
        delays.append(int(int(splitted[2])) - int(splitted[1]))

    mean = statistics.mean(delays)
    stdev = statistics.stdev(delays)
    print("{}: mean: {} stdev: {}".format(filename, mean, stdev))
    return mean, stdev

def plot_bar_chart(data, stdev, labels, filename):
    plt.clf()

    plt.figure(figsize=(4, 3))

    axes = plt.gca()
    axes.set_ylim([-250, 760])
    axes.set_ylabel(r'Time in $\mu s$')
    axes.set_xlabel('Number of GCMI Apps')
    axes.yaxis.grid(True)

    y_pos = np.arange(len(labels))

    # Create bars
    plt.bar(y_pos, data, yerr=stdev, align='center', ecolor='black', capsize=6, width=0.5, hatch="//", fill=False, edgecolor='black')

    # Create names on the x-axis
    plt.xticks(y_pos, labels)


    handles, labels = axes.get_legend_handles_labels()
    handles.append(plt.errorbar(0, -1000, yerr=10, ecolor="black", fmt='none', elinewidth=1, capsize=4, capthick=1))
    labels.append("Standard Deviation")

    legend = axes.legend()
    legend._legend_box = None
    legend._init_legend_box(handles, labels)
    legend._set_loc(legend._loc)
    legend.set_title(legend.get_title().get_text())


    plt.savefig(filename, bbox_inches='tight')

def plot_line_chart_matching_messages(data, labels, filename):
    plt.clf()

    plt.figure(figsize=(4, 3))

    axes = plt.gca()
    #axes.set_ylim([30, 250])
    axes.set_ylabel(r'Processing Time in $\mu s$')
    axes.set_xlabel('Matching Messages')
    axes.yaxis.grid(True)

    line_names = ["1", "2", "4", "8", "16"]

    y_pos = np.arange(len(labels))

    df = pd.DataFrame({'x': np.arange(len(labels)), line_names[0]: data[0], line_names[1]: data[1],
                       line_names[2]: data[2], line_names[3]: data[3], line_names[4]: data[4]})

    # Create lines
    colors = ['darkgreen', 'blue', 'orange', 'purple', 'red']
    for i, line_name in reversed(list(enumerate(line_names))):
        plt.plot( 'x', line_name, data=df, marker='.', markerfacecolor=colors[i], markersize=6, color=colors[i], linewidth=1)

    # Create names on the x-axis
    plt.xticks(y_pos, labels)

    plt.rcParams['legend.title_fontsize'] = '9'

    # Shrink current axis by 20%
    box = axes.get_position()
    axes.set_position([box.x0, box.y0, box.width * 0.8, box.height])

    # Put a legend to the right of the current axis
    axes.legend(title='Number of\nGCMI Apps', loc='center left', bbox_to_anchor=(1, 0.5))

    plt.savefig(filename, bbox_inches='tight')

def plot_line_chart_with_without_tls(data, labels, filename):
    plt.clf()

    plt.figure(figsize=(4, 3))

    axes = plt.gca()
    axes.set_ylim([0, 175])
    axes.set_ylabel(r'processing time in $\mu s$')
    axes.set_xlabel('number of apps')

    y_pos = np.arange(len(labels))

    df = pd.DataFrame({'x': np.arange(len(labels)), 'with TLS': data[0], 'without TLS': data[1]})

    # Create lines
    plt.plot( 'x', 'with TLS', data=df, marker='.', markerfacecolor='black', markersize=5, color='blue', linewidth=1)
    plt.plot( 'x', 'without TLS', data=df, marker='.', markerfacecolor='black', markersize=5, color='darkgreen', linewidth=1)
    plt.legend()

    # Create names on the x-axis
    plt.xticks(y_pos, labels)

    plt.savefig(filename, bbox_inches='tight')

def plot_line_chart_filter_apps(data, labels, filename):
    plt.clf()

    plt.figure(figsize=(3.5, 3.5))

    axes = plt.gca()
    #axes.set_ylim([0, 8000])
    axes.set_ylabel(r'Time in $\mu s$')
    axes.set_xlabel('Number of Filters')
    axes.yaxis.grid(True)

    y_pos = np.arange(len(labels))

    df = pd.DataFrame({'x': np.arange(len(labels)), 'Filters in one GCMI App': data[0], 'Filters in different\nGCMI Apps': data[1]})

    # Create lines
    plt.plot( 'x', 'Filters in one GCMI App', data=df, marker='.', markerfacecolor='darkgreen', markersize=5, color='darkgreen', linewidth=1)
    plt.plot( 'x', 'Filters in different\nGCMI Apps', data=df, marker='.', markerfacecolor='blue', markersize=5, color='blue', linewidth=1)
    plt.legend()

    # Create names on the x-axis
    plt.xticks(y_pos, labels)

    plt.savefig(filename, bbox_inches='tight')

def plot_herter_vs_enhanced_bar(data, stdev):
    plt.clf()
    plt.figure(figsize=(5, 4))

    colors = ["#3498db", "#95a5a6", "#2ecc71"]
    bar_labels = ['Enhanced Framework\nwith one Proxy', 'Enhanced Framework\nwith multiple Proxies', 'Herter\'s Framework']

    barWidth = 0.25
    barPositions = range(len(data[0]))

    for i, bars in enumerate(data):
        currentBarPositions = [x + i * barWidth for x in barPositions]
        plt.bar(currentBarPositions, bars, error_kw=dict(lw=1, capsize=4, capthick=1), width=barWidth, color=colors[i], yerr=stdev[i], label=bar_labels[i], ecolor="black")

    axes = plt.gca()
    handles, labels = axes.get_legend_handles_labels()
    handles.append(plt.errorbar(0, -1000, yerr=10, ecolor="black", fmt='none', elinewidth=1, capsize=4, capthick=1))
    labels.append("Standard Deviation")

    legend = axes.legend()
    legend._legend_box = None
    legend._init_legend_box(handles, labels)
    legend._set_loc(legend._loc)
    legend.set_title(legend.get_title().get_text())

    plt.xticks([r + barWidth for r in range(len(data[0]))], [1, 2, 4, 8, 16])
    axes.yaxis.grid(True)
    axes.set_ylim([-150, 3600])
    axes.set_ylabel(r'Time in $\mu s$')
    axes.set_xlabel('Number of GCMI Apps')

    # Show graphic
    plt.savefig("enhanced_vs_herter.pdf", bbox_inches='tight')

def plot_line_chart_caches_same_messages(data, labels, filename):
    plt.clf()

    plt.figure(figsize=(3.5, 3.5))

    axes = plt.gca()
    #axes.set_ylim([0, 2500])
    axes.set_ylabel(r'Time in $\mu s$')
    axes.set_xlabel('Percentage of cacheable Messages')
    axes.yaxis.grid(True)

    y_pos = np.arange(len(labels))

    df = pd.DataFrame({'x': np.arange(len(labels)), 'With Cache': data[0], 'Without Cache': data[1]})

    # Create lines
    plt.plot('x', 'With Cache', data=df, marker='.', markerfacecolor='darkgreen', markersize=5, color='darkgreen',
             linewidth=1)
    plt.plot('x', 'Without Cache', data=df, marker='.', markerfacecolor='blue', markersize=5, color='blue',
             linewidth=1)
    plt.legend()

    # Create names on the x-axis
    plt.xticks(y_pos, labels)

    plt.savefig(filename, bbox_inches='tight')

def plot_double_line_chart_caches_same_messages(data, labels, filename):
    plt.clf()

    plt.figure(figsize=(4, 4))

    axes = plt.gca()
    #axes.set_ylim([0, 2500])
    axes.set_ylabel(r'Time in $\mu s$')
    axes.set_xlabel('Percentage of cacheable Messages')
    axes.yaxis.grid(True)

    y_pos = np.arange(len(labels))

    df = pd.DataFrame({'x': np.arange(len(labels)), '1000 Filters\nwith Cache': data[0], '1000 Filters\nwithout Cache': data[1],
                       '4000 Filters\nwith Cache': data[2], '4000 Filters\nwithout Cache': data[3],
                       '8000 Filters\nwith Cache': data[4], '8000 Filters\nwithout Cache': data[5]})

    print('1000 without cache on avg: {}'.format(statistics.mean(data[1])))
    print('4000 without cache on avg: {}'.format(statistics.mean(data[3])))
    print('8000 without cache on avg: {}'.format(statistics.mean(data[5])))

    # Create lines
    plt.plot('x', '8000 Filters\nwith Cache', data=df, marker='.', markerfacecolor='#B80028', markersize=5,
             color='#B80028',
             linewidth=1)
    plt.plot('x', '8000 Filters\nwithout Cache', data=df, marker='.', markerfacecolor='#FF9900', markersize=5, color='#FF9900',
             linewidth=1)
    plt.plot('x', '4000 Filters\nwith Cache', data=df, marker='.', markerfacecolor='#6A8347', markersize=5,
             color='#6A8347',
             linewidth=1)
    plt.plot('x', '4000 Filters\nwithout Cache', data=df, marker='.', markerfacecolor='#A6CB45', markersize=5, color='#A6CB45',
             linewidth=1)
    plt.plot('x', '1000 Filters\nwith Cache', data=df, marker='.', markerfacecolor='#005B9A', markersize=5, color='#005B9A',
             linewidth=1)
    plt.plot('x', '1000 Filters\nwithout Cache', data=df, marker='.', markerfacecolor='#74C2E1', markersize=5, color='#74C2E1',
             linewidth=1)

    box = axes.get_position()
    axes.set_position([box.x0, box.y0, box.width * 0.8, box.height])

    # Put a legend to the right of the current axis
    axes.legend(fontsize='9', loc='center left', bbox_to_anchor=(1, 0.5))

    # Create names on the x-axis
    plt.xticks(y_pos, labels)

    plt.savefig(filename, bbox_inches='tight')

def plot_throughput_filter(data, labels):
    plt.clf()

    plt.figure(figsize=(4.5, 3))

    axes = plt.gca()
    #axes.set_ylim([0, 80000])
    axes.set_ylabel('Responses per s')
    axes.set_xlabel('Number of GCMI Apps')
    axes.yaxis.grid(True)

    y_pos = np.arange(len(labels))

    line_names = ['0%', '20%', '40%', '60%', '80%', '100%']

    df = pd.DataFrame({'x': np.arange(len(labels)), line_names[0]: data[0], line_names[1]: data[1],
                       line_names[2]: data[2], line_names[3]: data[3], line_names[4]: data[4], line_names[5]: data[5]})

    # Create lines
    colors = ['darkgreen', 'blue', 'orange', 'purple', 'red', 'pink']
    for i, line_name in enumerate(line_names):
        plt.plot('x', line_name, data=df, marker='.', markerfacecolor=colors[i], markersize=5, color=colors[i], linewidth=1)

    # Shrink current axis by 20%
    box = axes.get_position()
    axes.set_position([box.x0, box.y0, box.width * 0.8, box.height])

    # Put a legend to the right of the current axis
    axes.legend(title='matching\nmessages', fontsize = '9', loc='center left', bbox_to_anchor=(1, 0.5))

    # Create names on the x-axis
    plt.xticks(y_pos, labels)

    plt.savefig('scenario_t3_result.pdf', bbox_inches='tight')

if __name__ == "__main__":
    args = sys.argv[2:]
    mode = sys.argv[1:][0]

    if mode == "apps":
        directory = args[0]

        mean_data_points = []
        stdev_data_points = []
        labels = []

        for number_of_apps in [1, 2, 4, 8, 16]:
            mean, stdev = get_results_for_file(directory + "/proxy_times_{}.txt".format(number_of_apps))
            mean_data_points.append(mean)
            stdev_data_points.append(stdev)
            labels.append(str(number_of_apps))

        plot_bar_chart(mean_data_points, stdev_data_points, labels, "scenario_d4_result.pdf")

    elif mode == "matching_messages":
        directory = args[0]
        numbers_of_apps = [1, 2, 4, 8, 16]
        matching_ratios = [0, 20, 40, 60, 80, 100]
        labels = []
        mean_data_lines = []

        for matching_ratio in matching_ratios:
            labels.append(str(matching_ratio) + "%")

        for number_of_apps in numbers_of_apps:
            mean_data_points = []
            for matching_ratio in matching_ratios:
                mean, stdev = get_results_for_file(
                    directory + "/{}_percent_matching/proxy_times_{}.txt".format(matching_ratio, number_of_apps))
                mean_data_points.append(mean)

            mean_data_lines.append(mean_data_points)

        plot_line_chart_matching_messages(mean_data_lines, labels, 'plot_matching.pdf')

    elif mode == "with_without_tls":
        directory = args[0]

        mean_data_lines = []
        labels = [1, 2, 4, 8, 16]

        for subdirectory in [directory + '/with_tls', directory + '/without_tls']:
            mean_data_points = []
            for number_of_apps in labels:
                mean, stdev = get_results_for_file(subdirectory + "/proxy_times_{}.txt".format(number_of_apps))
                mean_data_points.append(mean)

            mean_data_lines.append(mean_data_points)

        plot_line_chart_with_without_tls(mean_data_lines, labels, 'plot_tls.pdf')

    elif mode == "filter_in_one_multiple_apps":
        labels = [1000, 2000, 4000, 8000, 16000]
        mean_data_lines = []

        for directory in args:
            mean_data_points = []
            for number_of_apps in [1, 2, 4, 8, 16]:
                mean, stdev = get_results_for_file(directory + "/proxy_times_{}.txt".format(number_of_apps))
                mean_data_points.append(mean)

            mean_data_lines.append(mean_data_points)

        plot_line_chart_filter_apps(mean_data_lines, labels, 'plot_filter_apps.pdf')

    elif mode == "filter_caches_same_messages":
        mean_data_lines = []
        labels = [0, 20, 40, 60, 80, 100]

        for directory in args:
            mean_data_points = []
            for number_of_apps in labels:
                mean, stdev = get_results_for_file(directory + "/proxy_times_cache_{}.txt".format(number_of_apps))
                mean_data_points.append(mean)

            mean_data_lines.append(mean_data_points)

        plot_double_line_chart_caches_same_messages(mean_data_lines, labels, 'plot_no_vs_hashmap_cache.pdf')

    elif mode == "throughput_filter":
        labels = [1, 2, 4, 8, 16]
        data_lines = []
        directory = args[0]

        subfolder_names = []
        for matching_messages in [0, 20, 40, 60, 80, 100]:
            subfolder_names.append('/{}_percent_matching'.format(matching_messages))

        filenames = []
        for number_of_apps in [1, 2, 4, 8, 16]:
            filenames.append('cbench_throughput_{}.log'.format(number_of_apps))

        for subfolder_name in subfolder_names:
            data_line = []
            for filename in filenames:
                data_line.append(get_avg_from_cbench_result(directory + subfolder_name + "/" + filename))
            data_lines.append(data_line)

        print(data_lines)
        plot_throughput_filter(data_lines, labels)

    elif mode == "herter":
        labels = [1, 2, 4, 8, 16]
        directory = args[0]
        data_lines = []
        data_lines_stdev = []

        for mode in ["enhanced_services", "enhanced_proxychain", "herter"]:
            data_line = []
            data_line_stdev = []
            for label in labels:
                mean, stdev = get_pybench_result(directory + "/cbench_times_{}_{}.txt".format(mode, label))
                data_line.append(mean)
                data_line_stdev.append(stdev)

            data_lines.append(data_line)
            data_lines_stdev.append(data_line_stdev)

        plot_herter_vs_enhanced_bar(data_lines, data_lines_stdev)