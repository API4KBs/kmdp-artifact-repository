package edu.mayo.kmdp.repository.artifact.jcr;


import java.io.Closeable;
import javax.jcr.Session;

public class DaoResult<T> implements Closeable {

  private T value;
  private Session session;

  DaoResult(T value, Session session) {
    this.value = value;
    this.session = session;
  }

  @Override
  public void close() {
    this.session.logout();
  }

  public T getValue() {
    return value;
  }

  public Session getSession() {
    return session;
  }
}
