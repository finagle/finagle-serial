#@namespace scala i.g.f.s.thriftscala

struct Small {
  1: list<bool> booleans;
  2: string string;
}

exception NameRecognizerException {
  1: string description;
}

service EchoService {
  Small echo(1: Small s)
}
