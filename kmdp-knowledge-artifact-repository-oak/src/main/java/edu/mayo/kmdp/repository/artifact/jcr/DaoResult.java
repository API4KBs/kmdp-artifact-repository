package edu.mayo.kmdp.repository.artifact.jcr;

import edu.mayo.kmdp.repository.artifact.dao.Artifact;
import java.util.List;
import java.util.function.Function;
import javax.jcr.Session;

public class DaoResult<T> implements edu.mayo.kmdp.repository.artifact.dao.DaoResult<T> {

  private T value;
  private Session session;

  DaoResult(T value, Session session) {
    this.value = value;
    this.session = session;
  }

  @Override
  public void close() {
    session.logout();
  }

  public T getValue() {
    return value;
  }

  public Session getSession() {
    return session;
  }

  public <X> edu.mayo.kmdp.repository.artifact.dao.DaoResult<X> map(Function<T,X> mapper) {
    return new DaoResult<>(mapper.apply(value),session);
  }
}

