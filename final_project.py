import os
import glob
import time
from bluetooth import*
import RPi.GPIO as GPIO

server_sock = BluetoothSocket(RFCOMM)
server_sock.bind(("", PORT_ANY))
server_sock.listen(1)

uuid = "00001101-0000-1000-8000-00805f9b34fb"

advertise_service(server_sock, "LillyMPiServer" ,
                  service_id = uuid,
                  service_classes = [uuid, SERIAL_PORT_CLASS],
                  profiles = [SERIAL_PORT_PROFILE],
                  )

os.system('modprobe w1-gpio')
os.system('modprobe w1-therm')
base_dir = '/sys/bus/w1/devices/'
device_folder = glob.glob(base_dir + '28*')[0]
device_file = device_folder + '/w1_slave'

def read_temp_raw():
    f = open (device_file, 'r')
    lines = f.readlines()
    f.close()
    return lines
def read_temp():
    lines = read_temp_raw()
    while lines [0] .strip()[-3:] != 'YES' :
        time.sleep(0.2)
        lines = read_temp_raw()
    equals_pos = lines[1].find('t=')
    if equals_pos != -1:
        temp_string = lines[1][equals_pos+2:]
        temp_c = float(temp_string) / 1000.0
        temp_f = temp_c * 9.0 / 5.0 + 32.0
        return temp_c, temp_f

while True:
    print "Waiting for connection on RFCOMM"
    client_sock, client_info = server_sock.accept()

    print "Accepted connection from:" , client_info
    try:
        data = client_sock.recv(1024)
        if len(data) == 0: break
        print "recieved [%s]" % data
        if data == 'Work':
            client_sock.send("%s,%s" % (temp_f,temp_c))
            print(read_temp())
            time.sleep(5)
    except Exception as e:
        pass

