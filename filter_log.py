import sys 

filters = ['ActivityThread', 'BufferQueue', 'NativeCrypto', 'SurfaceTextureClient', 'AppCrawler', 'ExtSolo', 'System.out', 'SurfaceFlinger']
def is_filtered(line):
    for f in filters:
        if f in line:
            return False
    return True
  
with open(sys.argv[1]) as f:
    for line in filter(is_filtered, f.readlines()):
        sys.stdout.write(line)
