import sys
from bs4 import BeautifulSoup

f = open(sys.argv[1])
xml_info = f.read()
f.close()
soup = BeautifulSoup(xml_info, "html.parser")
login = soup.find_all("login")
if login is None:
	print("Error")
else:
	print(login[0]["hostname"].replace('\\\"',""))
