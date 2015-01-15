from distutils.core import setup

setup(
    name='OpenMatrix',
    version='0.2.2',
    author='Billy Charlton',
    author_email='billy@worldofbilly.com',
    packages=['openmatrix', 'openmatrix.test'],
    url='http://pypi.python.org/pypi/OpenMatrix/',
    license='LICENSE.txt',
    description='OMX, the open matrix data format.',
    long_description=open('README.txt').read(),
    install_requires=[
        "tables >= 2.3.0",
        "numpy >= 1.5.0",
    ],
)
