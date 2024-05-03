import 'package:flutter/material.dart';
import 'dart:math';

void main() {
  runApp(RandomLetterApp());
}

class RandomLetterApp extends StatefulWidget {
  @override
  _RandomLetterAppState createState() => _RandomLetterAppState();
}

class _RandomLetterAppState extends State<RandomLetterApp> {
  String currentLetter = _getRandomLetter();

  static String _getRandomLetter() {
    final random = Random();
    final alphabet = 'abcdefghijklmnopqrstuvwxyz';
    final index = random.nextInt(alphabet.length);
    final isUpperCase = random.nextBool();
    final letter = alphabet[index];
    return isUpperCase ? letter.toUpperCase() : letter;
  }

  void _updateLetter() {
    setState(() {
      currentLetter = _getRandomLetter();
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: GestureDetector(
          onTap: _updateLetter,
          child: Container(
            padding: EdgeInsets.all(10.0), // 1cm padding (10 pixels)
            color: Colors.white,
            child: Center(
              child: Text(
                currentLetter,
                style: TextStyle(
                  fontSize: MediaQuery.of(context).size.height * 0.6, // Fill the screen height
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
