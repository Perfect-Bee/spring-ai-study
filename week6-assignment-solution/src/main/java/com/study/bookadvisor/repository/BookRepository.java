package com.study.bookadvisor.repository;

import com.study.bookadvisor.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByGenre(String genre);

}
