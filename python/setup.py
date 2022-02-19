# pip install voteutil
# pip install voteutil[xlsx]

from setuptools import setup

setup(
    name='voteutil',
    version='0.1.0',
    description='vote util',
    author='Brian Olson',
    author_email='bolson@bolson.org',
    url='https://github.com/brianolson/voteutil',
    packages=['voteutil'],
    package_dir={'voteutil': 'voteutil'},
    entry_points={
        'console_scripts':
        [
            'countvotes = voteutil.countvotes:main',
            'xlsxtocsv = voteutil.xlsxtocsv:main',
        ]
    },
    extras_require={
        'xlsx':['openpyxl'],
    },
    license='BSD',
    classifiers=[
        'Programming Language :: Python :: 3.8',
    ]
)
