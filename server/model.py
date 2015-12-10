from peewee import *

db = SqliteDatabase('sec-app')

class BaseModel(Model):

    class Meta:
        database = db

class EncryptedFile(BaseModel):
    filename = CharField(unique=True)
    maskedIndexString = CharField()
    
def showFiles():
    encFiles = EncryptedFile.select().execute()
    print "Index" + "\t" + "FileName"
    for index,f in enumerate(encFiles):
        print str(index) + "\t" + str(f.filename)


