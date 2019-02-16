namespace java thrift.test

struct User
{
    1: i32 id,
    2: string name
}

service ThriftTest
{
    User getUser(1: i32 id)
}