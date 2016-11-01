package op

def file =  new File('version/apk/', req.requestURI.replace('/upgrade', ''))
println file
if (file.exists() && file.isFile()) {
    render file
} else {
    render 'not found!'
}
