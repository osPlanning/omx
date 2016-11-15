from distutils.core import setup

setup(
    name='OpenMatrix',
    version='0.2.3',
    author='Billy Charlton',
    author_email='billy@worldofbilly.com',
    packages=['openmatrix', 'openmatrix.test'],
    url='https://sites.google.com/site/openmodeldata',
    license='Apache',
    description='OMX, the open matrix data format.',
    long_description=open('README.txt').read(),
    install_requires=[
        "tables >= 3.0.0",
        "numpy >= 1.5.0",
    ],
    classifiers=[
        'License :: OSI Approved :: Apache Software License'
    ]
)
