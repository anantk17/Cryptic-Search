import os
from flask import Flask,request,redirect,url_for,jsonify,send_from_directory,abort
from werkzeug import secure_filename
import hmac
import sha
import hashlib
from model import *
import base64


UPLOAD_FOLDER = '/home/anant/College/Computer-Security/Project/Code/server/uploads'
ALLOWED_EXTENSIONS = set(['enc'])
files = []

app = Flask(__name__)

def create_tables():
    db.connect()
    db.create_table(EncryptedFile)


def allowed_file(filename):
    return '.' in filename and \
            filename.rsplit('.',1)[1] in ALLOWED_EXTENSIONS

"""
    Store file passed via POST request on server in UPLOAD_FOLDER 
"""

"""@app.route('/app/upload/',methods=['POST'])
def upload_file():
    if request.method == 'POST':
        file = request.files['file']
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            file.save(os.path.join(app.config['UPLOAD_FOLDER'],filename))
            return jsonify({}),200
"""
"""
    Store (filename,index) in files
    File should already exist in UPLOAD_FOLDER 
    If file not already uploaded, return 404 error
"""

@app.route('/app/index/<string:filename>',methods=['POST'])
def upload_string(filename):
    if request.method == 'POST':
        indexString = request.form['indexString']
        fileContents = request.form['fileData']

        #data = base64.b64decode(fileContents)
        data = fileContents
        outputFileName = os.path.join(app.config['UPLOAD_FOLDER'],filename)
        with open(outputFileName, "w") as text_file:
            text_file.write(data)

        index = map(int,list(indexString))
        #files.append((filename,index))
        encFile = EncryptedFile(filename = filename, maskedIndexString = indexString)
        encFile.save()
        return jsonify({}),200
    else:
        abort(404)

"""
    Given parameters f and p, returns the list of files that have the requested keyword
"""

@app.route('/app/query',methods=['GET'])
def query():
    p = int(request.args.get('p',''))
    f = request.args.get('f','')
    f = str(f)
    
    print p, f
    list_of_files = []
    
    def convert_int_to_bytearray(val):
        import bitstring
        stream = bitstring.BitString("int:32="+str(val))
        while(len(stream)!=160):
            stream.append("0x00")
        
        return bytearray(stream.bytes)


    def G(f,j):
        h = hmac.new(f,indexBytes,sha)
        #h.update(hex(j))
        return int(h.hexdigest(),16)%2
    
    for index,encFile in enumerate(EncryptedFile.select()):
        #Gbit = G(f,index)
        f_hex_data = f.decode('hex')
        f_bytes = bytearray(f_hex_data)
        #print f_bytes
        print index
        indexBytes = convert_int_to_bytearray(index)
        print indexBytes                
        Gbit = G(f_bytes,indexBytes)
        print Gbit
        Mbit = int(encFile.maskedIndexString[p])
        print Mbit

        if Mbit ^ Gbit:
            list_of_files.append(encFile.filename)
    
    print list_of_files

    return jsonify({'filenames':list_of_files}),200

"""
    Serve requested file to client
"""

@app.route('/app/upload/<path:filename>',methods=['GET'])
def serve_file(filename):
    root_dir = os.path.dirname(os.getcwd())
    return send_from_directory(directory=UPLOAD_FOLDER,filename=filename)

@app.route('/app/file/<path:filename>',methods=['GET'])
def send_encoded_file(filename):
    rawFileName = os.path.join(app.config['UPLOAD_FOLDER'],filename)
    with open(rawFileName,"rb") as rawFile:
        rawString = rawFile.read()
    
    #encodedString = base64.b64encode(rawString)
    return jsonify({'fileData':rawString}),200


app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
if __name__ == '__main__':
    app.run(debug=True,host='0.0.0.0',port=9090)
