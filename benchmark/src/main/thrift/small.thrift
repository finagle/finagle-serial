#@namespace scala io.github.finagle.serial.benchmark.thriftscala

struct Small {
  1: list<bool> booleans;
  2: string body;
}

service EchoService {
  Small echo(1: Small s)
}
