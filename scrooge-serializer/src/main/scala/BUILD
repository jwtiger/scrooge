scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'scrooge-serializer',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty:libthrift-0.9.1',
    'util/util-codec',
    'util/util-core',
    'scrooge/scrooge-core'
  ],
  sources=rglobs('*.scala'),
)
