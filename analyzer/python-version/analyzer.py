import sys
import struct
import time
from sklearn.cluster import KMeans
import pandas as pd
import numpy as np
from collections import Counter, defaultdict
from sklearn import datasets
import operator
from sklearn import linear_model
from sklearn.metrics import r2_score
from sklearn import preprocessing

def find_segments_by_kmeans(key, df_col, lowerBound, upperBound):
	kmeans = KMeans(n_clusters = 25, n_init = 3, n_jobs = -1)
	lowerNumber = lowerBound * len(df_col)
	upperNumber = upperBound * len(df_col)
	kmeans.fit(df_col)
	centroid = kmeans.cluster_centers_.tolist()
	centroid_num = Counter(kmeans.labels_)
	res = {}
	removed = []
	removed_req = []

	for i in range(0, len(centroid)):
		# special case: due to duplicated values, the number of real clusters is smaller than k.
		if centroid[i][0] not in res:
			res[centroid[i][0]] = centroid_num[i]
	sorted_key = sorted(res.keys(), reverse = True)
	incor_num = 0
	found = 0
	for kindex in sorted_key:
		# if the number of elements in a cluster is 0, no need to further consider
		# optimize
		if res[kindex] == 0:
			continue
		if (incor_num + res[kindex] >= lowerNumber and incor_num + res[kindex] <= upperNumber):
			incor_num += res[kindex]
			removed.append(centroid.index([kindex]))
			break
		elif incor_num + res[kindex] > upperNumber:
			break
		elif incor_num + res[kindex] < lowerNumber:
			incor_num += res[kindex]
			removed.append(centroid.index([kindex]))
	
	print("%s removed = %f" % (key, incor_num / len(df_col)))
	for i in range(0, len(df_col)):
		if (kmeans.labels_.item(i) in removed):
			removed_req.append(i)
	return removed_req


np.seterr(all='raise')
CPU_FREQ_NS = 2.2
NUM_VAR = 14
EVENT_NAME = {0:"RUNNABLE", 1:"WAITING", 2:"HARDIRQ", 3:"SOFTIRQ", 4:"EXITTOUSER", 5:"MEMMIGRATION", 6:"INST", 7:"CYCLE", 8:"KINST", 9:"KCYCLE", 10:"RUNNING", 11:"1_Freq"}
MSR_NAME = set()

f_trace = open(sys.argv[1], "rb")
f_switch = open(sys.argv[2], "r")
f_msrinfo = open(sys.argv[3], "r")
f_twindow = int(sys.argv[4])

#First step, impact value
#Record switch info for each round, including start time and end time
switch_info = []
for l in f_switch:
	l = l.split()
	stime = float(l[4])/CPU_FREQ_NS
	etime = float(l[5])/CPU_FREQ_NS
	switch_info.append((stime, etime))

#Record MSR information in each round.
msr_info = []
msr_tmp_info = []
for l in f_msrinfo:
	if "Level" not in l:
		continue
	l = l.split()
	msr_tmp_info.append(l[-6])
	MSR_NAME.add(l[-6])
	if len(msr_tmp_info) == 4:
		msr_tmp_info.append(int(l[-8]))
		msr_info.append(msr_tmp_info)
		msr_tmp_info = []	

#TODO: Split by the time window
lat_event_res = {}
for key in EVENT_NAME.values():
	lat_event_res[key] = []
for key in MSR_NAME:
	lat_event_res[key] = []
pair_events = {}
#print(lat_event_res.keys())

#Parse trace file
stime = time.time()
try:
	while True:
		#ThreadID
		threadId = struct.unpack('i', f_trace.read(4))[0]
		#RequestID
		requestId = struct.unpack('i', f_trace.read(4))[0]
		#EventType
		eventType = struct.unpack('b', f_trace.read(1))[0]
		#RequestType = 
		requestType = struct.unpack('b', f_trace.read(1))[0]
		#BitMap
		bitMap = struct.unpack('h', f_trace.read(2))[0]
		
		#Get values of the bitmap
		tmp_results = []
		for i in range(0, NUM_VAR):
			if ((bitMap >> i) & 1) == 1:
				value = struct.unpack('l', f_trace.read(8))[0]
				if ((i >= 0 and i <= 4) or i >= 12):
					value /= CPU_FREQ_NS
				tmp_results.append(value)
			else:
				tmp_results.append(0)
		
		#Get round information
		switch_idx = -1
		latency = tmp_results[13] + tmp_results[0] + tmp_results[1]
		for i in range(1, len(switch_info)):
			if tmp_results[12] > switch_info[i - 1][1] and tmp_results[12] + latency < switch_info[i][0]:
				switch_idx = i - 1
				break
		#print(switch_info)
		if switch_idx == -1:
			#print("not found switch info", tmp_results[12], tmp_results[12] + latency, switch_info[0][0], switch_info[0][1])
			continue
		
		#Get other important values
		runningLength = tmp_results[13] - tmp_results[2] + tmp_results[3] - tmp_results[4] - tmp_results[5]
		normalFreq = 1.0 * tmp_results[7] / runningLength
		inversedFreq = 1.0  /  normalFreq

		#Request type filter
		if requestType != 0:
			continue
		#Remove weird datapoints
		if runningLength < 0 or any(i < 0 for i in tmp_results) or normalFreq > 3.2:
			continue
		
		#Add values for runningLength and inversedFreq
		lat_event_res["RUNNING"].append((latency, runningLength))
		lat_event_res["1_Freq"].append((latency, inversedFreq))

		#Values before INST, CYCLE, and MSRs
		for i in range(0, 6):
			lat_event_res[EVENT_NAME[i]].append((latency, tmp_results[i]))
		
		#Values of INST, CYCLE
		for i in range(6, 8):
			tmpi = i
			if msr_info[switch_idx][4] == 1:
				tmpi += 2
			lat_event_res[EVENT_NAME[tmpi]].append((latency, tmp_results[i]))
		
		#Values of MSRs
		for i in range(8, 12):
			tmpi = i - 8
			lat_event_res[msr_info[switch_idx][tmpi]].append((latency, tmp_results[i]))

		#Add value pairs of two events
		#Inst/cycle and MSR pairs
		for i in range(6, 8):
			tmpi = i
			if msr_info[switch_idx][4] == 1:
				tmpi += 2
			for j in range(8, 12):
				tmpj = j - 8
				pair_events.setdefault((EVENT_NAME[tmpi], msr_info[switch_idx][tmpj]), []).append((tmp_results[i], tmp_results[j]))
		#MSR pairs                                           	
		for i in range(8, 11):
			tmpi = i - 8
			for j in range(i + 1, 12):
				tmpj = j - 8
				pair_events.setdefault((msr_info[switch_idx][tmpi], msr_info[switch_idx][tmpj]), []).append((tmp_results[i], tmp_results[j]))

except:
	print("[Error]", sys.exc_info()[0], sys.exc_info()[1])
	pass

etime = time.time()
print("[INFO] parsing file takes %f seconds." % (etime - stime))

#for k in lat_event_res.keys():
#	print (k, len(lat_event_res[k]))

#The first step to get impact value
stime = time.time()
P_target = 0.99
lowerBound = 0.02
upperBound = 0.03
results = {}
k = 25

for key in lat_event_res.keys():
	df_col = np.array([[ele[1]] for ele in lat_event_res[key]])
	removed = find_segments_by_kmeans(key, df_col, lowerBound, upperBound)

	left_req = []
	total_req = []
	df_lat = [ele[0] for ele in lat_event_res[key]]
	for i in range(0, len(df_lat)):
		if i not in removed:
			left_req.append(df_lat[i])
		total_req.append(df_lat[i])
	compare = [total_req, left_req]
	compare_results = []
	for lat in compare:
		req = []
		lat = sorted(lat, reverse = True)
		total_num = len(lat)
		#Prequest
		spos = (1 - P_target - 0.005) * total_num
		epos = (1 - P_target + 0.005) * total_num
		for i in range(0, total_num):
			if i >= spos and i <= epos:
				req.append(lat[i])
		compare_results.append(np.mean(req))
	results[key] = compare_results[0] / compare_results[1] - 1

sorted_results = sorted(results.items(), key=operator.itemgetter(1), reverse=True)
f_impact_value = open("impact_value.res", "w")
for key in sorted_results:
	f_impact_value.write('%s %f\n' % (key[0], key[1]))
f_impact_value.close()
etime = time.time()
print("[INFO] calculating impact value takes %f seconds." % (etime - stime))

'''
#Second step, proportional relationship
stime = time.time()
f_prop_relation = open("prop_relation.res", "w")
for key in pair_events:
	tmp_x = [[x[0]] for x in pair_events[key]]
	tmp_y = [[x[1]] for x in pair_events[key]]
	sorted_y = sorted(tmp_y, reverse=True)
	median_y = sorted_y[int(len(l) / 2)][0]
	model = linear_model.LinearRegression(n_jobs = -1)
	model.fit(tmp_x, tmp_y)
	f_prop_relation.write("%s %s %f %f\n" % (key[0], key[1], abs(model.intercept_[0]/median_y) if median_y > 0 else 0, model.score(tmp_x, tmp_y)))
f_prop_relation.close()
etime = time.time()
print("[INFO] finding proportional relationship takes %f seconds." % (etime - stime))

#Third step, jaccard similarity
stime = time.time()
f_jaccard_simi = open("jaccard_simi.res", "w")
for key in pair_events:
	df_col = []
	removed_res = []
	df_col.append([[ele[0]] for ele in pair_events[key]])
	df_col.append([[ele[1]] for ele in pair_events[key]])
	total_num = len(df_col[0])
	removed_res.append(find_segments_by_kmeans(key[0], df_col[0], lowerBound, upperBound))
	removed_res.append(find_segments_by_kmeans(key[1], df_col[1], lowerBound, upperBound))
	intersection = len(list(set(removed_res[0]) & set(removed_res[1])))
	union = len(list(set().union(set(removed_res[0]), set(removed_res[1]))))
	f_jaccard_simi.write("%f %f %f\n" % (len(removed_res[0])/total_num, len(removed_res[1])/total_num, 1.0 * intersection/union if union > 0 else 0))
f_jaccard_simi.close()
etime = time.time()
print("[INFO] calculating jaccard similarity takes %f seconds." % (etime - stime))
'''

f_trace.close()
f_switch.close()
f_msrinfo.close()
