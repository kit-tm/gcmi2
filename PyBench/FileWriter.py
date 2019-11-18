class FileWriter():

    def find_xid_in_results(self, xid, results):
        xids = []
        for i, result in enumerate(results):
            if result[1] == xid:
                xids.append(i)
                if len(xids) == 2: break

        return xids

    def write_results_to_file(self, results):
        invalid_count = 0
        formatted_results = []

        while len(results) > 0:
            entries_with_xid = self.find_xid_in_results(results[0][1], results)

            if len(entries_with_xid) != 2:
                invalid_count += 1
                print(str(len(entries_with_xid)) + " entries for xid " + str(results[0][1]))
            elif results[entries_with_xid[0]][0] > results[entries_with_xid[1]][0]:
                formatted_results.append([results[0][1], results[entries_with_xid[1]][0], results[entries_with_xid[0]][0]])
            else:
                formatted_results.append([results[0][1], results[entries_with_xid[0]][0], results[entries_with_xid[1]][0]])

            entries_with_xid.sort(reverse=True)
            for i in entries_with_xid: del results[i]


        f = open("cbench_times.txt", "a+")
        for measurement in formatted_results:
            f.write(str(measurement[0]) + " " + str(measurement[1]) + " " + str(measurement[2]) + "\n")
        f.flush()
        f.close()

        print("saved " + str(len(formatted_results)) + " results!")
        print(str(invalid_count) + " invalid measurements")