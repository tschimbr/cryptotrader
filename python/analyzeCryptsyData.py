import json
from pprint import pprint
import sys
import os

folder = sys.argv[1]
print "Analyzing data from folder " + folder

os.chdir(folder)
archive_files = sorted([f for f in os.listdir(folder) if f.endswith('.tar.gz')])

for file in archive_files:
	print "Analyzing tar.gz file: " + file
	os.system("rm -R ALL_MARKETS")
	os.system("tar -zxf " + file)
	json_files = sorted([f for f in os.listdir("ALL_MARKETS") if f.endswith('.json')])
	for json_file in json_files:
		print "Analyzing JSON file: " + json_file


#json_data=open('json_data')

#data = json.load(json_data)
#pprint(data)
#json_data.close()
