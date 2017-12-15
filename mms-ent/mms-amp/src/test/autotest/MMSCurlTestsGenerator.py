#!/usr/bin/python
# Auto-generate CURL commands to run against MMS.
from xml.dom.minidom import parse
from pprint import pprint
import collections
import commands
import re
import string

''' 
IDENTIFIERS/IDs:
Add new ID:value pair if desired inside defaultIDs dictionary. 
ID's are those inside curly braces in class2Url.html (not queries/those with '?' next to them)
ID's that are not explicitly defined will be given the value in the variable: ALL_OTHER_ID
'''
defaultIDs = {
	'workspaceId':'master',
	'siteId':'Regression_Test_Site',
	'projectId':'Regression_Test_Project',
	'viewId':'Regression_Test_View',
	'siteName':'Europa_Regression_Test'
}
ALL_OTHER_ID = 'Test_'


'''
QUERIES/PARAMETERS:
If desired, add new parameter information (ex: queries and all their possible values) in paramValues dictionary
make sure values are always a list
'''
#TBD: Might automate this process (by searching relevant JS or Java files to find values of these parameters)
paramValues = {
	'recurse':['true','false'],
	'fix':['true','false'],
	'validate':['true','false'],
	'doc':['true','false'],
	'force':['true','false'],
	'product':['true','false'],
	'user': ['Random_User_ID'],
	'background':['true','false'],
	'delete':['true','false'],
	'on': ['true','false'],
	'off': ['true','false'],
	'cs':['Random_CS_ID'],
	'extension': ['Random_Extension_ID'],
	'keyword':['Random_ID_To_Search_7201969','ExistingElementID'], #ExistingElementID will automatically be replaced with an existing element
	'timestamp':['no','< E', '< last', 'current', '> current']
}


'''
DATA (for Post Requests):
If needed, update the json files that is used to post to a particular URL 
'''
jsonData = {
	'configurations':'configuration.json',
	'elements':'elementsNew.json',
	'products':'product.json',
	'views':'views.json',
	'projects':'{\"name\":\"Regression_Test_1\"}'
}


'''
TBD!!:
Test Procedure for each URL:
	to test GET request: simply call a GET request, with all possible combinations of queries
	to test POST request: (1) GET item to post, (2) POST item, (3) GET item to verify item was posted
	to test DELETE request: (1) GET an existing item, (2) DELETE item, (3) GET item to verify item was deleted
'''

# Variable Dec:
CLASS2URL = 'class2url.html'
CLASS2URL_PATH = '../../../bin/'
OUTPUT_BASHFILENAME = 'GeneratedCurlTests.sh'

urlList = []
out = open (OUTPUT_BASHFILENAME,'w')
getList = []
postList = []
deleteList = []

def main ():
	# open class2Url.html
	f = open (CLASS2URL_PATH+CLASS2URL,'r')

	# parse html file
	for line in f:
		url = getCellValue(line,0)
		descXml = getCellValue (line,1)
		bean = getCellValue (line,2)

		if url is not None and url.strip() != 'None' and bean.strip() != 'None' and bean is not None:
			urlList.append([url,descXml,bean])

	sortURLsToRequests()
	
	createHeaderAndFlags()
	createAllGetRequests()
	createAllPostRequests()
	createAllDeleteRequests()

# Create Post Requests
def createAllPostRequests():
	out.write('\n\n######################################## POST REQUESTS ########################################\n')
	out.write('echo')
	out.write('\necho POST REQUESTS:\n')
	for url in postList:
		# create comment describing request
		s = '\n# Post '+ getElementedActedOn(url) + '\n'
		newUrl = replaceIDs(url)
		url2 = replaceParams (newUrl)
		# create curl command
		s += 'echo curl $CURL_FLAGS $CURL_POST_FLAGS $SERVICE_URL @JsonData/[TBD :D ] \"'+url2+'\"\n'
		out.write(s)
	out.write('echo')


# Create Get Requests
def createAllGetRequests():
	out.write('\n\n######################################## GET REQUESTS ########################################\n')
	out.write('echo')
	out.write('\necho GET REQUESTS:\n')
	for url in getList:
		# create comment describing request
		s = '\n# get '+ getElementedActedOn(url) + '\n'
		newUrl = replaceIDs(url)
		url2 = replaceParams (newUrl)
		#generateAllCurlCombinations (newUrl)
		# create curl command
		s += 'echo curl $CURL_FLAGS $CURL_GET_FLAGS $SERVICE_URL\"'+url2+'\"\n'
		out.write(s)
	out.write('echo')


# Create Delete Requests
def createAllDeleteRequests():
	out.write('\n\n######################################## DELETE REQUESTS ########################################\n')
	out.write('echo')
	out.write('\necho DELETE REQUESTS:\n')
	for url in deleteList:
		# create comment describing request
		s = '\n# get '+ getElementedActedOn(url) + '\n'
		newUrl = replaceIDs(url)
		url2 = replaceParams (newUrl)
		# create curl command
		s += 'echo curl $CURL_FLAGS $CURL_DELETE_FLAGS $SERVICE_URL\"'+url2+'\"\n'
		out.write(s)
	out.write('echo')

# replace all parameters with values and provide all possible combinations
#def generateAllCurlCombinations (url):

# replace the parameters/queries with values in paramValues
def replaceParams(url):
	try:
		regex = '(\{(.*?)\})'
		print url
		matchList = re.findall(regex,url)
		for item in matchList:
			idWithBrkts = item[0]
			idName = item[1]
			if '?' in idName:
				idWoutQMarks = idName.replace('?','')
				url = url.replace(idWithBrkts,','.join(paramValues.get(idWoutQMarks)))
				print '\t'+url
		return url + '\\'
	except:
		return None
	
# obtain the last value in the URL to see what the CRUD request is acting on
def getElementedActedOn(url):
	entries = url.split('/')
	return entries[len(entries)-1] 

# replace id's enclosed in { } with their appropriate values (see global variables)
def replaceIDs (url):
	try:
		regex = '(\{(.*?)\})'
		matchList = re.findall(regex,url)
		for item in matchList:
			idWithBrkts = item[0]
			idName = item[1]
			if '?' not in idName:
				if defaultIDs.get(idName) is not None:
					url = re.sub(idWithBrkts,defaultIDs.get(idName), url)
				elif idName is not None:
					url = re.sub(idWithBrkts,ALL_OTHER_ID+idName, url)
		return url + '\\'
	except:
		return None

# Parse descriptor files to place URLs into their corresponding request Lists (getList, postList, and deleteList)
# URLs may fall under multiple requests if they have more than one descriptor files/request functionalities
def sortURLsToRequests ():
	for key in urlList:
		descriptorFileName = key[1]
		regex = '.*\.(.*)\.desc\.xml\s*'
		p = re.search (regex,descriptorFileName)
		if (p is not None):
			request = p.group(1)
			if (request.strip().lower() == 'get'):
				getList.append(key[0])
			elif (request.strip().lower() == 'post'):
				postList.append(key[0])
			elif (request.strip().lower() == 'delete'):
				deleteList.append(key[0])

# creates bash file header and flags ()
def createHeaderAndFlags ():
	s =  '#!/bin/bash\n'
	s += '# Bash file containing CURL commands generated from MMSCurlTestsGenerator.py. Based on curl.tests.sh and diff2.sh.\n'
	s += '\nmkdir -p TestsOutput\n'
	s += 'passedTest=0\n'
	s += '\nexport CURL_STATUS=\'-w \\n%{http_code}\\n\'\n'
	s += 'export CURL_POST_FLAGS_NO_DATA="-X POST"\n'
	s += 'export CURL_POST_FLAGS=\'-X POST -H Content-Type:application/json --data\'\n'
	s += 'export CURL_PUT_FLAGS="-X PUT"\n'
	s += 'export CURL_GET_FLAGS="-X GET"\n'
	s += '\nexport CURL_USER=" -u admin:admin"\n'
	s += 'export CURL_FLAGS=$CURL_STATUS$CURL_USER\n'
	s += 'export SERVICE_URL="http://localhost:8080/alfresco/service/"\n\n'
	out.write(s)

# get the cell/column from a html line
def getCellValue (line,loc):
	try:
		regex = '\s*<\s*td\s*>(.*?)<\s*/td\s*>\s*'
		s = re.findall(regex,line)
		return s.pop(loc)
	except:
		return None

# execute command and return results
def execCmd(cmd):
  results = commands.getoutput(cmd)
  return results.split('\n')

if __name__ == "__main__":
	main()