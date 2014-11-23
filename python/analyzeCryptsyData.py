import json
from pprint import pprint
import sys
import os

folder = sys.argv[1]
print "Analyzing data from folder " + folder

os.chdir(folder)
archive_files = sorted([f for f in os.listdir(folder) if f.endswith('.tar.gz')])

analysis = {}
one_day_average = {}

for file in archive_files:
	print "Analyzing tar.gz file: " + file
	os.system("rm -R ALL_MARKETS")
	os.system("tar -zxf " + file)
	json_files = sorted([f for f in os.listdir("ALL_MARKETS") if f.endswith('.json')])
	for json_file in json_files:
		print "Analyzing JSON file: " + json_file
		json_data = open("ALL_MARKETS/" + json_file)
		market_info = json.load(json_data)
		markets = market_info['return']['markets']
		market_keys = markets.keys()
		pprint(markets[market_keys[0]])
		
		json_data.close()
		exit()
		


# print analysis to file
markets = analysis.keys()

output = open(sys.argv[2], 'w')
for market in markets:
	output.write(market + ";")
	for day in markets[markets]:
		output.write(day + ";")
output.writeln()


output.close()
