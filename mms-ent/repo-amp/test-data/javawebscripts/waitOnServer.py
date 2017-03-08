import sys
import regression_lib

##########################################################################################    
#
# MAIN METHOD 
#
##########################################################################################    
if __name__ == '__main__':
    if len(sys.argv) > 2 and sys.argv[1] == "-f":
        filename = sys.argv[2]
        if filename != None:
            filename.strip()
            regression_lib.wait_on_server(filename)
    else:
        regression_lib.wait_on_server()