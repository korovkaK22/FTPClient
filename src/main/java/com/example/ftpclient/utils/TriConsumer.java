package com.example.ftpclient.utils;

import java.io.IOException;

@FunctionalInterface
public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c) throws IOException;
}
