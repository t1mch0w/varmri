import sys
from bs4 import BeautifulSoup

f = open(sys.argv[1])
xml_info = f.read()
f.close()
soup = BeautifulSoup(xml_info, "html.parser")
hosts = soup.find_all("login")
if hosts is None or len(hosts) == 0:
	print("Error")
else:
	for host in hosts:
		#print(host["username"])
		if host["username"] == '\\"fzhou\\"/':
			print(host["hostname"].replace('\\\"',""))
