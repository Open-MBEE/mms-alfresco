#!/usr/bin/python
from xml.dom.minidom import parse
from pprint import pprint
import collections
import commands

CONTEXT_PATH = '../src/main/amp/config/alfresco/module/mms-amp/context/'

CONTEXT_FILES = [CONTEXT_PATH + 'javawebscript-service-context.xml',
				 CONTEXT_PATH + 'mms-service-context.xml']

DESC_PATH = '../src/main/amp/config/alfresco/extension/templates/webscripts/'
url2class = {}
urlBacked = set([])

def main():
	#parseAllJs()
	for filename in CONTEXT_FILES:
		parseContext(filename)
	parseAllDescFiles()

	od = collections.OrderedDict(sorted(url2class.items()))

	html = open('class2url.html', 'w')
	html.write('<html><body><style>table,th,td{border:1px solid black;border-collapse:collapse}</style><table>\n')
	html.write('<tr>  <th>URL</th>  <th>Descriptor File</th>  <th>Bean Class or Javascript</th></tr>\n')
	for key, value in od.items():
		for entry in value:
			descFile = entry[0][entry[0].find('jpl/')+4:]
			# if entry[1].endswith('js'):
			# 	beanClass = entry[1][entry[1].find('jpl/')+4:]
			# else:
			# 	beanClass = entry[1][entry[1].rfind('.')+1:]
			if entry[1]:
				beanClass = entry[1]
			else:
				beanClass = 'None'
			if not key:
				key = 'None'
			html.write('<tr><td>' + key + '</td><td>' + descFile + '</td><td>' + beanClass + '</td></tr>\n')
	html.write('</body></table></html>\n')
	html.close()


def parseContext(filename):
	'''
	Parse all the Spring configuration files that provide the
	beans for all the Java backed webscripts.

	This sets the global url2class dictionary which is of the form:
	{
	url: [descriptor filename, Java class],
	...
	}

	@param	filename Full path of Spring configuration file
	'''
	dom = parse(filename)
	beans = dom.getElementsByTagName('bean')
	for bean in beans:
		beanClass = bean.getAttribute('class')
		beanId = bean.getAttribute('id')
        if beanId:
			descFile = convertBeanId2DescFile(beanId)
			url = getUrlFromDesc(descFile)
			if not url2class.has_key(url):
				url2class[url] = []
				urlBacked.add(url)
			url2class[url].append([descFile, beanClass])


def getUrlFromDesc(descFile):
	'''
	Returns the URL for the specified descriptor file.
	'''
	try:
		dom = parse(descFile)
		urls = dom.getElementsByTagName('url')
		urlString = urls[0].toxml().replace('<url>','').replace('</url>','')
		print urlString, descFile
		return urlString
	except:
		return None


def convertBeanId2DescFile(id):
	'''
	Converts the bean id attribute in the Spring configuration file to
	a fully qualified path for the descriptor file.
	'''
	id = id.replace('webscript.', '')
	tokens = id.split('.')

	filename = DESC_PATH
        ii = None
	for ii in range(len(tokens)-1):
		filename += tokens[ii] + '/'
        if ii == None:
	        print "couldn't parse", id
        else:
	        filename = filename[:-1] + '.' + tokens[ii+1] + '.desc.xml'

	return filename


def parseAllJs():
	'''
	Parse all the JS files in the webscripts directories and add
	them to the url2class dictionary
	'''
	cmd = 'find ' + DESC_PATH[:-1] + ' -name "*.js"'
	filenames = execCmd(cmd)

	for jsFile in filenames:
		# find the first occurrence of js (includes JSON format)
		descFile = jsFile[:jsFile.index('js')] + 'desc.xml'
		url = getUrlFromDesc(descFile)

		if url:
			if not url2class.has_key(url):
				url2class[url] = []
				urlBacked.add(url)
			url2class[url].append([descFile, jsFile])
		else:
			print jsFile, descFile


def parseAllDescFiles():
	'''
	Parse all the descriptor files that aren't backed by JS or
	Java files and inject them into url2class
	'''
	cmd = 'find ' + DESC_PATH[:-1] + ' -name "*.desc.xml"'
	filenames = execCmd(cmd)
	for descFile in filenames:
		url = getUrlFromDesc(descFile)

		if url:
			# print descFile, url
			if url not in urlBacked:
				if not url2class.has_key(url):
					url2class[url] = []
				url2class[url].append([descFile, None])


# execute command and return results
def execCmd(cmd):
  results = commands.getoutput(cmd)
  return results.split('\n')


if __name__ == "__main__":
	main()

