How to install and run paperballots

python3 -mvenv ve
. ve/bin/activate
pip install Jinja2
python paperballots/render.py -i paperballots/testballot.json -d /tmp
# view /tmp/ballot.html in your browser
